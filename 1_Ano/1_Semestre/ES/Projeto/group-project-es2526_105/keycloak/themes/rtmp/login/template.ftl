<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    
    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/logo.svg" />
    
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body class="${bodyClass}">
    <div id="kc-container-wrapper">
        <div id="kc-container">
            <div id="kc-header">
                <div id="kc-logo">
                    <div id="kc-logo-wrapper">
                        <img src="${url.resourcesPath}/img/logo.svg" alt="RTMP Logo" />
                    </div>
                </div>
                <div id="kc-header-wrapper">
                    Risk & Threat Modelling Platform
                </div>
            </div>

            <div id="kc-content">
                <div id="kc-content-wrapper">
                    <#-- App-initiated actions should not see warning messages about the need to complete the action during login. -->
                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="alert alert-${message.type}">
                            <#if message.type = 'success'><span>✓</span></#if>
                            <#if message.type = 'warning'><span>⚠</span></#if>
                            <#if message.type = 'error'><span>✕</span></#if>
                            <#if message.type = 'info'><span>ℹ</span></#if>
                            <span>${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#nested "form">

                    <#if auth?has_content && auth.showTryAnotherWayLink()>
                        <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                            <div id="kc-form-options">
                                <div></div>
                                <div>
                                    <input type="hidden" name="tryAnotherWay" value="on"/>
                                    <a href="#" id="try-another-way"
                                       onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                                </div>
                            </div>
                        </form>
                    </#if>

                    <#nested "info">

                    <#nested "socialProviders">
                </div>
            </div>
        </div>
    </div>
</body>
</html>
</#macro>
