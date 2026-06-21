package org.jahia.community.whatismypublicserverid.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("What is my public server IP queries")
public class WhatIsMyPublicServerIpQueryExtension {

    private WhatIsMyPublicServerIpQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("whatIsMyPublicServerIp")
    @GraphQLDescription("What is my public server IP query namespace")
    public static WhatIsMyPublicServerIpQuery whatIsMyPublicServerIp() {
        return new WhatIsMyPublicServerIpQuery();
    }
}
