import React from 'react';
import {useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Loader, Typography} from '@jahia/moonstone';
import styles from './WhatIsMyPublicServerIp.scss';
import {GET_WHAT_IS_MY_PUBLIC_SERVER_IP} from './WhatIsMyPublicServerIp.gql';

export function WhatIsMyPublicServerIp() {
    const {t} = useTranslation('what-is-my-public-server-ip');
    const {data, loading, error} = useQuery(GET_WHAT_IS_MY_PUBLIC_SERVER_IP, {fetchPolicy: 'network-only'});

    React.useEffect(() => {
        document.title = `${t('label')} — Jahia Administration`;
    }, [t]);

    return (
        <div className={styles.container}>
            {/* Two fixed-role live regions — AT caches role at registration; dynamic mutation is ignored */}
            <div role="status" aria-live="polite" aria-atomic="true" className={styles.wip_sr_only}>
                {loading ? t('whatIsMyPublicServerIp.loading') : ''}
            </div>
            <div role="alert" aria-live="assertive" aria-atomic="true" className={styles.wip_sr_only}>
                {error ? t('whatIsMyPublicServerIp.error') : ''}
            </div>

            {loading && (
                <div className={styles.loaderWrapper} aria-hidden="true">
                    <Loader size="big"/>
                </div>
            )}

            {error && (
                <div aria-hidden="true" className={styles.wip_errorBanner}>
                    {t('whatIsMyPublicServerIp.error')}
                </div>
            )}

            {!loading && !error && (
                <div className={styles.card}>
                    <div className={styles.cardHeader}>
                        <Typography component="h2" variant="subheading" weight="semiBold" isUpperCase style={{color: '#fff'}}>
                            {t('label')}
                        </Typography>
                    </div>
                    <div className={styles.cardBody}>
                        <dl className={styles.ipDefinition}>
                            <dt className={styles.ipLabel}>{t('whatIsMyPublicServerIp.title')}</dt>
                            <dd className={styles.ipValue}>
                                {data?.whatIsMyPublicServerIp?.ip}
                            </dd>
                        </dl>
                    </div>
                </div>
            )}
        </div>
    );
}

export default WhatIsMyPublicServerIp;
