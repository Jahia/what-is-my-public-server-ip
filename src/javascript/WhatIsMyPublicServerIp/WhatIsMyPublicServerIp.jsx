import React from 'react';
import {useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Banner, Loader, Typography} from '@jahia/moonstone';
import styles from './WhatIsMyPublicServerIp.scss';
import {GET_WHAT_IS_MY_PUBLIC_SERVER_IP} from './WhatIsMyPublicServerIp.gql';

export function WhatIsMyPublicServerIp() {
    const {t} = useTranslation('what-is-my-public-server-ip');
    const {data, loading, error} = useQuery(GET_WHAT_IS_MY_PUBLIC_SERVER_IP, {fetchPolicy: 'network-only'});

    if (loading) {
        return (
            <div className={styles.loaderWrapper}>
                <Loader size="big"/>
            </div>
        );
    }

    if (error) {
        return (
            <div className={styles.container}>
                <Banner variant="danger" title={t('whatIsMyPublicServerIp.error')}>
                    {error.message}
                </Banner>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <div className={styles.cardHeader}>
                    <Typography variant="subheading" weight="semiBold" isUpperCase style={{color: '#fff'}}>
                        {t('label')}
                    </Typography>
                </div>
                <div className={styles.cardBody}>
                    <span className={styles.ipLabel}>{t('whatIsMyPublicServerIp.title')}</span>
                    <span className={styles.ipValue}>
                        {data && data.whatIsMyPublicServerIp}
                    </span>
                </div>
            </div>
        </div>
    );
}

export default WhatIsMyPublicServerIp;
