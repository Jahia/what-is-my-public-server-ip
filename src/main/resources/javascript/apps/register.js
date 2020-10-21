window.jahia.i18n.loadNamespaces('what-is-my-public-server-ip');

window.jahia.uiExtender.registry.add('adminRoute', 'whatIsMyPublicServerIp', {
    targets: ['administration-server-configuration:10'],
    requiredPermission: 'adminSystemInfos',
    label: 'what-is-my-public-server-ip:label',
    isSelectable: true,
    iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/$lang/settings.whatIsMyPublicServerIp.html'
});