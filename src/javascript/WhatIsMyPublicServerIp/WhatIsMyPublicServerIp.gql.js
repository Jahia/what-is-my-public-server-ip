import {gql} from '@apollo/client';

export const GET_WHAT_IS_MY_PUBLIC_SERVER_IP = gql`
    query {
        whatIsMyPublicServerIp
    }
`;
