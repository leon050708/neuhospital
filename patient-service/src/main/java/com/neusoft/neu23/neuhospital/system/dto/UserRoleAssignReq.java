package com.neusoft.neu23.neuhospital.system.dto;

import java.util.List;

public class UserRoleAssignReq {

    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
