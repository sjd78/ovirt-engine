package org.ovirt.engine.core.bll.adbroker;

import java.util.EnumMap;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

public class SearchQueryFotmatter implements LdapQueryFormatter<LdapQueryExecution> {

    private final EnumMap<SearchLangageLDAPTokens, String> tokensToLDAPKeys;

    public SearchQueryFotmatter(EnumMap<SearchLangageLDAPTokens, String> tokensToLDAPKeys) {
        this.tokensToLDAPKeys = tokensToLDAPKeys;
    }

    /**
     * Replace the keywords generated by the SyntaxChecker class with the provider-type specific LDAP query. The
     * replacement is basically identifying Dollar sign, "$" as a prefix for the token and then fetching the token value
     * from the map. e.g. an search expression like this (&($LDAP_USER_ACCOUNT)($GIVENNAME=John)) should look
     * $LDAP_USER_ACCOUNT in
     */
    @Override
    public LdapQueryExecution format(LdapQueryMetadata queryMetadata) {

        String filter = (String) queryMetadata.getQueryData().getFilterParameters()[0];
        for (Entry<SearchLangageLDAPTokens, String> tokenEntry : tokensToLDAPKeys.entrySet()) {
            filter = StringUtils.replace(filter, tokenEntry.getKey().name(), tokenEntry.getValue());
        }
        String baseDN = queryMetadata.getBaseDN();

        return new LdapQueryExecution(filter,
                        filter, // The display filter in that case is like the filter
                baseDN,
                        queryMetadata.getContextMapper(),
                        queryMetadata.getSearchScope(),
                        queryMetadata.getReturningAttributes(),
                        queryMetadata.getQueryData().getDomain());
    }
}
