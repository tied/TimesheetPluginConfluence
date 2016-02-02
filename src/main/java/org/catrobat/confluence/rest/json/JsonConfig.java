/*
 * Copyright 2014 Stephan Fellhofer
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

package org.catrobat.confluence.rest.json;

import org.catrobat.confluence.activeobjects.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unused")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class JsonConfig {

    @XmlElement
    private List<JsonTeam> teams;
    @XmlElement
    private List<String> approvedGroups;
    @XmlElement
    private List<String> approvedUsers;
    @XmlElement
    private long userDirectoryId;
    @XmlElement
    private String mailFromName;
    @XmlElement
    private String mailFrom;
    @XmlElement
    private String mailSubject;
    @XmlElement
    private String mailBody;

    public JsonConfig() {

    }

    public JsonConfig(AdminHelperConfigService configService) {
        AdminHelperConfig toCopy = configService.getConfiguration();

        Map<String, JsonTeam> teamMap = new TreeMap<String, JsonTeam>();
        for (Team team : toCopy.getTeams()) {
            teamMap.put(team.getTeamName(), new JsonTeam(team, configService));
        }

        this.teams = new ArrayList<JsonTeam>();
        this.teams.addAll(teamMap.values());

        //ToDO: not correct
        this.approvedUsers = new ArrayList<String>();
        for (ApprovedUser approvedUser : toCopy.getApprovedUsers()) {
            if (approvedUser.getUserKey() != null) {
                approvedUsers.add(approvedUser.getUserKey());
            }
        }

        this.approvedGroups = new ArrayList<String>();
        for (ApprovedGroup approvedGroup : toCopy.getApprovedGroups()) {
            approvedGroups.add(approvedGroup.getGroupName());
        }

        this.userDirectoryId = toCopy.getUserDirectoryId();
        this.mailFromName = toCopy.getMailFromName();
        this.mailFrom = toCopy.getMailFrom();
        this.mailSubject = toCopy.getMailSubject();
        this.mailBody = toCopy.getMailBody();
    }

    public List<JsonTeam> getTeams() {
        return teams;
    }

    public void setTeams(List<JsonTeam> teams) {
        this.teams = teams;
    }

    public List<String> getApprovedGroups() {
        return approvedGroups;
    }

    public void setApprovedGroups(List<String> approvedGroups) {
        this.approvedGroups = approvedGroups;
    }

    public List<String> getApprovedUsers() {
        return approvedUsers;
    }

    public void setApprovedUsers(List<String> approvedUsers) {
        this.approvedUsers = approvedUsers;
    }

    public long getUserDirectoryId() {
        return userDirectoryId;
    }

    public void setUserDirectoryId(long userDirectoryId) {
        this.userDirectoryId = userDirectoryId;
    }

    public String getMailFromName() {
        return mailFromName;
    }

    public void setMailFromName(String mailFromName) {
        this.mailFromName = mailFromName;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    public String getMailBody() {
        return mailBody;
    }

    public void setMailBody(String mailBody) {
        this.mailBody = mailBody;
    }
}