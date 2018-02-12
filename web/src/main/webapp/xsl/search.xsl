<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:geonet="http://www.fao.org/geonetwork"
	xmlns:exslt="http://exslt.org/common" exclude-result-prefixes="exslt geonet">
	<xsl:include href="metadata/common.xsl" />
	<xsl:output omit-xml-declaration="no" method="html"
		doctype-public="html" indent="yes" encoding="UTF-8" />
	<xsl:variable name="hostUrl" select="concat(/root/gui/env/server/protocol, '://', /root/gui/env/server/host, ':', /root/gui/env/server/port)"/>
	<xsl:variable name="baseUrl" select="/root/gui/url" />
	<xsl:variable name="serviceUrl" select="concat($hostUrl, /root/gui/locService)" />
	<xsl:variable name="rssUrl" select="concat($serviceUrl, '/rss.search?sortBy=changeDate')" />
	<xsl:variable name="siteName" select="/root/gui/env/site/name"/>
	
	<!-- main page -->
	<xsl:template match="/">
    <html class="no-js">
           <xsl:attribute name="lang">
                <xsl:value-of select="/root/gui/language" />
            </xsl:attribute>
    
			<head>
				<meta http-equiv="Content-type" content="text/html;charset=UTF-8"></meta>
				<meta http-equiv="X-UA-Compatible" content="IE=9,chrome=1"></meta>
				<title><xsl:value-of select="$siteName" /></title>
				<meta name="description" content="" ></meta>
                <meta name="viewport" content="width=device-width"></meta>
				<meta name="og:title" content="{$siteName}"/>
        <meta name="dc.language" content="en-au" />
				
				<link rel="icon" type="image/gif" href="../../images/logos/favicon.gif" />
				<link rel="alternate" type="application/rss+xml" title="{$siteName} - RSS" href="{$rssUrl}"/>
				<link rel="search" href="{$serviceUrl}/portal.opensearch" type="application/opensearchdescription+xml" title="{$siteName}"/>

    		<link rel="stylesheet" href="{concat($baseUrl, '/static/geonetwork-client_css.css')}"></link> 
        <link rel="schema.dc" href="http://purl.org/dc/elements/1.1/" />
        <link rel="schema.dcterms" href="http://purl.org/dc/terms/" />

				 <script type="text/javascript">

					<!-- Added by Joseph/Alice for Google analytics code  -->
					(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
					(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new
					Date();a=s.createElement(o),
					m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
					})(window,document,'script','//www.google-analytics.com/analytics.js','ga');
				
					ga('create', 'UA-1501753-1', 'ga.gov.au');
					ga('send', 'pageview');
				
				<!-- var _gaq = _gaq || [];
					_gaq.push(['_setAccount', 'UA-36263643-1']);
					_gaq.push(['_trackPageview']);

					(function() {
					var ga = document.createElement('script');
					ga.type = 'text/javascript';
					ga.async = true;
					ga.src = ('https:' == document.location.protocol ? 'https://ssl' :
					'http://www') + '.google-analytics.com/ga.js';
					var s = document.getElementsByTagName('script')[0];
					s.parentNode.insertBefore(ga, s);
					})();
				-->
				</script>

                 <xsl:choose>
                     <xsl:when test="/root/gui/config/map/osm_map = 'true'">
                         <script>
                             var useOSMLayers = true;
                         </script>
                     </xsl:when>

                     <xsl:otherwise>
                         <script>
                             var useOSMLayers = false;
                         </script>
                     </xsl:otherwise>
                 </xsl:choose>

			</head>
			<body>

			<div class="grey">
					<a href="javascript:window.print();" id="printer-button"><i class="fa fa-print"></i><xsl:value-of select="/root/gui/strings/print-button"/></a>
					<!-- ======================= Commented by Joseph - To remove rss button ============== -->
					<!--<a id="rss-button" href="/geonetwork/srv/eng/rss.latest"><i class="fa fa-rss-square"></i><xsl:value-of select="/root/gui/strings/rss-button"/></a>-->
					<span id="login-stuff">
						<a id="user-button">
							<xsl:choose>
								<xsl:when test="string(/root/gui/session/userId)=''">
							  	<xsl:attribute name="href">javascript:toggleLogin();</xsl:attribute>
							 	</xsl:when>
								<xsl:otherwise>
							  	<xsl:attribute name="href">javascript:app.loginApp.logout();</xsl:attribute>
							 	</xsl:otherwise>
						  </xsl:choose>
							<i class="fa fa-user"></i>
							<span id="user-button_label">
								<xsl:choose>
									<xsl:when test="string(/root/gui/session/userId)=''">
										<xsl:value-of select="/root/gui/strings/signIn"/>
							 		</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="/root/gui/strings/signOut"/>
							 		</xsl:otherwise>
						  	</xsl:choose>
							</span>
						</a>
						<label id="username_label">
							<xsl:if test="string(/root/gui/session/userId)!=''">
								<xsl:value-of select="concat(/root/gui/session/username,' ')"/>
							</xsl:if>
						</label>
						<label id="name_label">
							<xsl:if test="string(/root/gui/session/userId)!=''">
								<xsl:value-of select="concat(/root/gui/session/name,' ',/root/gui/session/surname,' ')"/>
							</xsl:if>
						</label>
						<label id="profile_label">
							<xsl:if test="string(/root/gui/session/userId)!=''">
								<xsl:value-of select="concat('(',/root/gui/session/profile,')')"/>	
							</xsl:if>
						</label>
						<a href="javascript:catalogue.admin();" id="administration-button">
							<xsl:if test="string(/root/gui/session/profile)!='Administrator'">
								<xsl:attribute name="style">display:none;</xsl:attribute>
							</xsl:if>
							<i class="fa fa-wrench"></i>
							<xsl:value-of select="/root/gui/strings/admin"/>
						</a>
						<label id="newmetadata_label">
							<a href="javascript:catalogue.metadataEdit2(null, true);" id="newmetadata-button">
								<xsl:if test="string(/root/gui/session/profile)='Guest' or string(/root/gui/session/profile)='RegisteredUser'">
									<xsl:attribute name="style">display:none;</xsl:attribute>
								</xsl:if>
								<i class="fa fa-file-o"></i>
								<xsl:value-of select="/root/gui/strings/newmetadata-button"/>
							</a>
						</label>
						<script>function false_(){ return false; }</script>
						<form id="login-form" style="display: none;" onsubmit="return false_();">
							<div id="login_div">
								<label>User name:</label>
								<input type="text" id="username" name="username"/><br/>
								<label>Password: </label>
								<input type="password" id="password" name="password"/><br/>
								<input type="submit" id="login_button" value="Login"/>
					  	</div>
						</form>
				  </span>
					<!-- from here on, all elements are floated to the right so 
					     they are in reverse order -->
					<a id="help-button" target="_blank" href="http://intranet.ga.gov.au/int-bin/viewdoc.pl?RecId=D2016-14872">
						<i class="fa fa-question-circle"></i><xsl:value-of select="/root/gui/strings/help"/>
					</a>
					<a id="lang-button" href="javascript:toggle('lang-form');">
            <xsl:for-each select="/root/gui/config/languages/*">
              <xsl:variable name="lang" select="name(.)"/>
              <xsl:if test="/root/gui/language=$lang">
                <span id="current-lang"><xsl:value-of select="/root/gui/strings/*[name(.)=$lang]"/></span>&#160;<i class="fa fa-angle-double-down"></i>
              </xsl:if>
            </xsl:for-each>
						<div id="lang-form" style="display:none;"></div>
          </a>
			</div>
				
      <div id="page-container">  
				<div id="container">
					<div id="header">
					  <a href="http://www.ga.gov.au/" title="Navigate to the Geoscience Australia home page"><div id="logo"></div></a>
            			<div id="search-title" style="width: 500px">Data and Publications Search</div>
						<header class="wrapper clearfix">
							<div style="width: 100%; margin: 0 auto;">
								<nav id="nav">
									<ul id="main-navigation">
										<li>
											<a id="catalog-tab" class="selected" href="javascript:showSearch();">
												<xsl:value-of select="/root/gui/strings/porCatInfoTab" />
											</a>
										</li>
										<!--
										<li>
											<a id="map-tab" href="javascript:showBigMap();">
												<xsl:value-of select="/root/gui/strings/map_label" />
											</a>
										</li>
										-->
										<!-- <li>
											<a id="browse-tab" href="javascript:showBrowse();">
												<xsl:value-of select="'Browse'" />
											</a>
										</li> -->
										<li>
											<a id="about-tab" href="javascript:showAbout();">
												<xsl:value-of select="/root/gui/strings/about" />
											</a>
										</li>
									</ul>
								</nav>
							</div>
              <div id="ga-line"></div>
						</header>
					</div>
					<div style="padding-left:50px" id="issue"></div>
					<div id="main" style="min-height:700px;">
			        <div id="copy-clipboard-ie"></div>
                       <div id="share-capabilities" style="display:none">
                            <a id="custom-tweet-button" href="javascript:void(0);" target="_blank">
                                    <xsl:value-of select="/root/gui/strings/tweet" />
                            </a>
                            <div id="fb-button">
                           </div>
                       </div>
                       <div id="permalink-div" style="display:none"></div>
                        <div id="bread-crumb-app"></div>
                        <div id="search-form" style="display:none">
                            <fieldset id="search-form-fieldset">
                                <legend id="legend-search">
                                    <xsl:value-of select="/root/gui/strings/search" />
                                </legend>
                                <span id='fullTextField'></span>
                                <input type="button"
                                    onclick="Ext.getCmp('advanced-search-options-content-form').fireEvent('search');"
                                    onmouseover="Ext.get(this).addClass('hover');"
                                    onmouseout="Ext.get(this).removeClass('hover');"
                                    id="search-submit" class="form-submit" value="&#xf002;">
                                </input>
                                <button type="button"
                                    onclick="Ext.getCmp('advanced-search-options-content-form').fireEvent('reset');"
                                    class="md-mn-reset1">Reset</button>
                                <div class="form-dummy">
                                    <span><xsl:value-of select="/root/gui/strings/dummySearch" /></span>
	                                <div id="ck1"/>
	                                <div id="ck2"/>
	                                <div id="ck3"/>
                                </div>
                                
                                <div id="show-advanced" onclick="showAdvancedSearch()">
                                    <span class="button"><xsl:value-of select="/root/gui/strings/advancedOptions.show" />&#160;<i class="fa fa-angle-double-down fa-2x show-advanced-icon"></i></span>
                                </div>
                                <div id="hide-advanced" onclick="hideAdvancedSearch(true)" style="display: none;">
                                    <span class="button"><xsl:value-of select="/root/gui/strings/advancedOptions.hide" />&#160;<i class="fa fa-angle-double-up fa-2x hide-advanced-icon"></i></span>
                                </div>
                                <div id="advanced-search-options" >
                                    <div id="advanced-search-options-content"></div>
                                </div>
                            </fieldset>
                        </div>
					

	                    <div id="browser" style="display:none">
                        <aside class="tag-aside">
                          <div id="tags">
                            <header><h1><span><xsl:value-of select="/root/gui/strings/tag_label" /></span></h1></header>
                            <div id="cloud-tag"></div>
                          </div>
                        </aside>
                        <article>
                          <div>
                            <section>
                              <div id="latest-metadata">
                                <header><h1><span><xsl:value-of select="/root/gui/strings/latestDatasets" /></span></h1></header>
                              </div>
                              <div id="popular-metadata">
                                <header><h1><span><xsl:value-of select="/root/gui/strings/popularDatasets" /></span></h1></header>
                              </div>
                            </section>
                          </div>
                        </article>
                      </div>

	                    <div id="about" style="display:none;">
	                    	<div id="welcome-text">
	                     	  <xsl:copy-of select="/root/gui/strings/welcome.text"/>
											  </div>
	                    	<div id="about-text">
	                      	<xsl:copy-of select="/root/gui/strings/about.text"/>
                        </div>
												<div style="margin: 1.33em;">
													<strong><xsl:value-of select="concat(/root/gui/builddetails/name,' (',/root/gui/builddetails/version,')')"/></strong> Built on: <strong><xsl:value-of select="/root/gui/builddetails/timestamp"/></strong> Build number: <xsl:value-of select="/root/gui/builddetails/revision"/>
												</div>
                      </div>
	                    
						<div id="big-map-container" style="display:none;background: #000;">
							<div id="loadingIndicator"> <!-- will be hidden by nationalmap -->
    						<div class="loading-indicator">Loading ...</div>
							</div>
							<div id="nationalmapContainer" class="nationalmap-container">
								<div id="cesiumContainer" class="cesium-container"></div>
							</div>
						</div>
            <div id="metadata-container" style="display:none;">
							<div id="metadata-refresh-button" style="display:none;margin-top:20px;margin-left:20px;"></div>
              <div id="metadata-info">
							</div>
						</div>
						<div id="search-container" class="main wrapper clearfix">
							<div id="bread-crumb-div"></div>

							<aside id="main-aside" class="main-aside" style="display:none;">
								<header><xsl:value-of select="/root/gui/strings/filter" /></header>
								<div id="facets-panel-div"></div>
							</aside>
							<article>
								<div style="display:none;">
									<aside id="secondary-aside" class="secondary-aside" style="display:none;">
										<header><xsl:value-of select="/root/gui/strings/recentlyViewed" /></header>
                  	<div id="recent-viewed-div"></div>
                  	<div id="mini-map"></div>
									</aside>
								</div>
								<header>
								</header>
								<section>
									<div id="result-panel"></div>
								</section>
								<footer>
								</footer>
							</article>
						</div>
						<!-- .main .wrapper .clearfix -->
					</div>



					<div id="only_for_spiders">
						<xsl:for-each select="/root/*/record">
							<article>
								<xsl:attribute name="id"><xsl:value-of
									select="uuid" /></xsl:attribute>
								<xsl:apply-templates mode="elementEP"
									select="/root/*[name(.)!='gui' and name(.)!='request']">
									<xsl:with-param name="edit" select="false()" />
									<xsl:with-param name="uuid" select="uuid" />
								</xsl:apply-templates>
							</article>
						</xsl:for-each>
					</div>
					<!-- #main -->

					<footer id="footer">
            <!--<xsl:if test="/root/gui/config/html5ui-footer!='true'">
              <xsl:attribute name="style">display:none;</xsl:attribute>
            </xsl:if>-->
						<div class="footerWrapper">
							<div id="ccByLicense">
							  <a href="https://creativecommons.org/licenses/by/4.0/legalcode"
								target="_blank"
								title="Go to Creative Commons home page"> <img
								src="../../apps/html5ui/img/cc-by.png"
								height="20" width="107"
								alt="Creative Commons Logo"></img>
							  </a>
							</div>
							<div id="gaCopyright">
							  <a href="http://www.ga.gov.au/copyright"
								target="_blank"
								title="View Geoscience Australia copyright statement">Copyright</a>
							</div>
							<div id="gaDisclaimer">
							  <a href="http://www.ga.gov.au/disclaimer"
								target="_blank"
								title="View Geoscience Australia disclaimer web page">Disclaimer</a>
							</div>
							<div id="gaPrivacy">
							  <a href="http://www.ga.gov.au/privacy"
								target="_blank"
								title="View Geoscience Australia privacy policy">Privacy</a>
							</div>
							<div id="gaAccessibility">
							  <a href="http://www.ga.gov.au/accessibility"
								target="_blank"
								title="View Geoscience Australia accessibility statement">Accessibility</a>
							</div>
							<div id="gaInfoScheme">
							  <a href="http://www.ga.gov.au/ips"
								target="_blank"
								style="padding-top: 22px"
								title="View Geoscience Australia privacy statement">Information Publication Scheme
							  </a>
							</div>
							<div id="gaFOI">
							  <a href="http://www.ga.gov.au/ips/foi"
								target="_blank"
								title="View Geoscience Australia freedom of information statement">Freedom of Information</a>
							</div>
							<div id="gaContact">
								<a href="http://www.ga.gov.au/contact-us"
								target="_blank"
								title="Contact Geoscience Australia">Contact us</a>
							</div>
						</div>
					</footer>
				</div>

				<input type="hidden" id="x-history-field" />
				<iframe id="x-history-frame" height="0" width="0"></iframe>

				 <xsl:variable name="minimize">
				   <xsl:choose>
						 <xsl:when test="/root/request/debug">?minimize=false</xsl:when>
						 <xsl:otherwise></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>

				<script type="text/javascript" src="{concat($baseUrl, '/static/geonetwork-client-mini-nomap.js', $minimize)}"></script>

    		<script>L_PREFER_CANVAS = true;</script>
        </div>
		<script>

			var browser=get_browser();
			if(browser.name === 'Chrome'){
				if(browser.version === '64'){
					var div = document.getElementById("issue");  
					div.textContent = "FOR CHROME USERS PRESS CATALOG TAB";  
					var text = div.textContent;  
				}
			}

			function get_browser() {
				var ua=navigator.userAgent,tem,M=ua.match(/(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*(\d+)/i) || []; 
				if(/trident/i.test(M[1])){
					tem=/\brv[ :]+(\d+)/g.exec(ua) || []; 
					return {name:'IE',version:(tem[1]||'')};
					}   
				if(M[1]==='Chrome'){
					tem=ua.match(/\bOPR|Edge\/(\d+)/)
					if(tem!=null)   {return {name:'Opera', version:tem[1]};}
					}   
				M=M[2]? [M[1], M[2]]: [navigator.appName, navigator.appVersion, '-?'];
				if((tem=ua.match(/version\/(\d+)/i))!=null) {M.splice(1,1,tem[1]);}
				return {
				  name: M[0],
				  version: M[1]
				};
		 }

		</script>
		</body>
	</html>
	</xsl:template>
</xsl:stylesheet>
