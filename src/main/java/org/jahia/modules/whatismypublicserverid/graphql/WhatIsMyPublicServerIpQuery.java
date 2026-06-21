package org.jahia.modules.whatismypublicserverid.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.modules.whatismypublicserverid.PublicIp;

@GraphQLName("WhatIsMyPublicServerIpQuery")
@GraphQLDescription("What is my public server IP queries")
public class WhatIsMyPublicServerIpQuery {

    @GraphQLField
    @GraphQLName("ip")
    @GraphQLDescription("Get the public IP used by the server to access Internet")
    @GraphQLRequiresPermission("publicServerIpAdmin")
    public String getPublicIp() {
        return new PublicIp().get();
    }
}
