/*
 * Copyright 2016 Adrian Schnedlitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.confluence.rest;


import com.atlassian.confluence.core.service.NotAuthorizedException;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.manager.directory.DirectoryManager;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.catrobat.confluence.activeobjects.AdminHelperConfigService;
import org.catrobat.confluence.activeobjects.Category;
import org.catrobat.confluence.activeobjects.Team;
import org.catrobat.confluence.rest.json.JsonCategory;
import org.catrobat.confluence.rest.json.JsonConfig;
import org.catrobat.confluence.rest.json.JsonTeam;
import org.catrobat.confluence.services.CategoryService;
import org.catrobat.confluence.services.PermissionService;
import org.catrobat.confluence.services.TeamService;
import org.catrobat.confluence.services.impl.PermissionServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Path("/config")
@Produces({MediaType.APPLICATION_JSON})
public class ConfigResourceRest {
  private final AdminHelperConfigService configService;
  private final DirectoryManager directoryManager;
  private final UserManager userManager;
  private final UserAccessor userAccessor;
  private final TeamService teamService;
  private final CategoryService categoryService;
  private final PermissionService permissionService;

  public ConfigResourceRest(final UserManager userManager, final AdminHelperConfigService configService,
                            final DirectoryManager directoryManager, final TeamService teamService,
                            final UserAccessor userAccessor, final CategoryService categoryService,
                            final PermissionService permissionService) {
    this.configService = configService;
    this.directoryManager = directoryManager;
    this.teamService = teamService;
    this.userAccessor = userAccessor;
    this.userManager = userManager;
    this.categoryService = categoryService;
    this.permissionService = permissionService;
  }


  @GET
  @Path("/getCategories")
  public Response getCategories(@Context HttpServletRequest request) {

    List<JsonCategory> categories = new LinkedList<JsonCategory>();

    for(Category category : categoryService.all()) {
      categories.add(new JsonCategory(category.getID(), category.getName()));
    }

    return Response.ok(categories).build();
  }

  @GET
  @Path("/getTeams")
  public Response getTeams(@Context HttpServletRequest request) {

    List<JsonTeam> teams = new LinkedList<JsonTeam>();
    UserProfile user;

    try {
      user = permissionService.checkIfUserExists(request);
    } catch (NotAuthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
    }

    for(Team team : teamService.all()) {
      Category[] categories = team.getCategories();
      int[] categoryIDs = new int[categories.length];
      for(int i = 0; i < categories.length; i++) {
        categoryIDs[i] = categories[i].getID();
      }
      teams.add(new JsonTeam(team.getID(), team.getTeamName(), categoryIDs));
    }

    return Response.ok(teams).build();
  }

  @GET
  @Path("/getConfig")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConfig(@Context HttpServletRequest request) {

    /*ToDo: Refactor CheckPermission
    Response unauthorized = checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }
    */

    return Response.ok(new JsonConfig(configService)).build();
  }

  @GET
  @Path("/getDirectories")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDirectories(@Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    List<Directory> directoryList = directoryManager.findAllDirectories();
    List<JsonConfig> jsonDirectoryList = new ArrayList<JsonConfig>();
    for (Directory directory : directoryList) {
      JsonConfig config = new JsonConfig();
      config.setUserDirectoryId(directory.getId());
      jsonDirectoryList.add(config);
    }

    return Response.ok(jsonDirectoryList).build();
  }

  @GET
  @Path("/getTeamList")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTeamList(@Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    List<String> teamList = new ArrayList<String>();
    for (Team team : configService.getConfiguration().getTeams()) {
      teamList.add(team.getTeamName());
    }

    return Response.ok(teamList).build();
  }

  @PUT
  @Path("/saveConfig")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response setConfig(final JsonConfig jsonConfig, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    configService.editMail(jsonConfig.getMailFromName(), jsonConfig.getMailFrom(),
            jsonConfig.getMailSubject(), jsonConfig.getMailBody());

    if (jsonConfig.getApprovedGroups() != null) {
      configService.clearApprovedGroups();
      for (String approvedGroupName : jsonConfig.getApprovedGroups()) {
        configService.addApprovedGroup(approvedGroupName);
      }
    }

    if (jsonConfig.getApprovedUsers() != null) {
      configService.clearApprovedUsers();
      for (String approvedUserName : jsonConfig.getApprovedUsers()) {
        UserProfile userProfile = userManager.getUserProfile(approvedUserName);
        if (userProfile != null) {
          configService.addApprovedUser(userManager.getRemoteUser().getUserKey().toString());
        }
      }
    }

    if (jsonConfig.getTeams() != null) {
      for (JsonTeam jsonTeam : jsonConfig.getTeams()) {
        configService.removeTeam(jsonTeam.getTeamName());
        configService.addTeam(jsonTeam.getTeamName(), jsonTeam.getCoordinatorGroups(),
                jsonTeam.getSeniorGroups(), jsonTeam.getDeveloperGroups());
      }
    }

    return Response.noContent().build();
  }

  @PUT
  @Path("/addTeamPermission")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addTeamPermission(final String modifyTeam, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    boolean successful = configService.addTeam(modifyTeam, null, null, null) != null;

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }

  @PUT
  @Path("/editTeamPermission")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response editTeamPermission(final String[] teams, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    if (teams == null || teams.length != 2) {
      return Response.serverError().build();
    } else if (teams[1].trim().length() == 0) {
      return Response.serverError().entity("Team name must not be empty").build();
    } else if (teams[1].compareTo(teams[0]) == 0) {
      return Response.serverError().entity("New Team name must be different").build();
    }

    boolean successful = configService.editTeam(teams[0], teams[1]) != null;

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }

  @PUT
  @Path("/removeTeamPermission")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeTeamPermission(final String modifyTeam, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    boolean successful = configService.removeTeam(modifyTeam) != null;

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }

  @PUT
  @Path("/addCategory")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addCategory(final String modifyCategory, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    boolean successful = categoryService.add(modifyCategory) != null;

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }

  @PUT
  @Path("/removeCategory")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeCategory(final String modifyCategory, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    boolean successful = categoryService.removeCategory(modifyCategory);

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }

  @PUT
  @Path("/addTeam")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addTeam(final String modifyTeam, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    boolean successful = teamService.add(modifyTeam) != null;

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }

  @PUT
  @Path("/removeTeam")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeTeam(final String modifyTeam, @Context HttpServletRequest request) {
    Response unauthorized = permissionService.checkPermission(request);
    if (unauthorized != null) {
      return unauthorized;
    }

    boolean successful = teamService.removeTeam(modifyTeam);

    if (successful)
      return Response.noContent().build();

    return Response.serverError().build();
  }
}
