import {registry} from '@jahia/ui-extender';
import React from 'react';
import {WhatIsMyPublicServerIp} from './WhatIsMyPublicServerIp';

export default () => {
    console.debug('%c what-is-my-public-server-ip: activation in progress', 'color: #463CBA');
    registry.add('adminRoute', 'whatIsMyPublicServerIp', {
        targets: ['administration-server-configuration:10'],
        requiredPermission: 'adminSystemInfos',
        label: 'what-is-my-public-server-ip:label',
        isSelectable: true,
        render: () => React.createElement(WhatIsMyPublicServerIp)
    });
};
