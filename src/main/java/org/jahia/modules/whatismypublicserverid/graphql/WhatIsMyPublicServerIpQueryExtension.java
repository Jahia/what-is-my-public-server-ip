package org.jahia.modules.whatismypublicserverid.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.modules.whatismypublicserverid.PublicIp;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLName("WhatIsMyPublicServerIpQueries")
@GraphQLDescription("What is my public server IP queries")
public class WhatIsMyPublicServerIpQueryExtension {

    private WhatIsMyPublicServerIpQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("whatIsMyPublicServerIp")
    @GraphQLDescription("Get the public IP used by the server to access Internet")
    @GraphQLRequiresPermission("admin")
    public static String getPublicIp() {
        return new PublicIp().get();
    }
}
