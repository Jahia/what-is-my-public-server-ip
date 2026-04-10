import {registry} from '@jahia/ui-extender';
import register from './WhatIsMyPublicServerIp/register';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'what-is-my-public-server-ip', {
        targets: ['jahiaApp-init:50'],
        callback: async () => {
            await i18next.loadNamespaces('what-is-my-public-server-ip', () => {
                console.debug('%c what-is-my-public-server-ip: i18n namespace loaded', 'color: #463CBA');
            });
            register();
            console.debug('%c what-is-my-public-server-ip: activation completed', 'color: #463CBA');
        }
    });
}
