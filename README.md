jira-svn-plugin-instantupdate
=

This is a fork of the excellent [JIRA Subversion Plugin](https://studio.plugins.atlassian.com/wiki/display/SVN/Subversion+JIRA+plugin).  By default, the plugin will poll all of the SVN repositories it is aware of on a configurable interval, e.g. 15 minutes.  This works fine when the number of repositories is small, but does not scale well for large numbers of repositories, e.g. a company-wide JIRA instance.  Additionally, there are times in the software development lifecycle when it is helpful to have your commits display "instantly" in JIRA.  The obvious solution is to lower the polling timer, but for large repositories or large numbers of repositories, this can cause JIRA to grind to a halt while it attempts to reindex.  This fork allows a user to trigger an index "update" for an individual repository via SOAP message, bypassing the polling timer and instantly associating the commit with a particular JIRA issue.  This allows you to leave the polling timer set at something high, e.g. 30 minutes, but still get quick updates for select repositories. If desired, you can create a SOAP client that is triggered by a post-commit hook so that this functionality is automatically invoked each time a user commits.

Building the Plugin
=
Clone this repository and build the plugin with Maven:
```
$ cd atlassian-jira-subversion-plugin-0.10.5.4_01
$ mvn clean package
```
**WARNING**: The plugin's POM and the parent POM it inherits from are old and abuse Maven2 functionality.  You will want to use Maven2, **not** Maven3 to build the project.  Additionally, you may need to add Atlassian's Maven2 Repository ([https://maven.atlassian.com/repository/public](https://maven.atlassian.com/repository/public)) to your settings.xml file or your proxy (Artifactory, Nexus) in order to resolve some artifacts.

Using the Plugin
=
Install the plugin according to the [installation instructions](https://studio.plugins.atlassian.com/wiki/display/SVN/Subversion+JIRA+plugin), but use the Maven artifact (```/target/atlassian-jira-subversion-plugin-0.10.5.4_01.jar```) instead of the corresponding jar downloaded from the plugin website.  Additionally, in JIRA:
* Enable "Accept remote API calls" under Administration > General Configuration > Global Settings
* Enable the "JIRA RPC Plugin" under Administration > Plugins > System
 * Enable the "SOAP" module
 * Enable "xml-rpc" module
* Enable the "JIRA Subversion Plugin under Administration > Plugins > System
 * Enable the "svnSoapService" module
 * Enable the "SOAP" module

You can verify that everything is working by opening a web browser and making sure you see a WSDL at [http://yourjiraserver/rpc/soap/subversionsoapservice-v2?wsdl](http://localhost/rpc/soap/subversionsoapservice-v2?wsdl).

(Optional) Create a Post-Commit Hook
=
Now that the service is enabled, you can write a SOAP client in the language of your choice and invoke it.  In addition, you may opt to have a SVN post-commit hook invoke the service every time a user commits (or selectively).  

Here is a sample SOAP client, written using JAX-WS:
```
/**
* This class is invoked by a SVN post-commit hook, and sends a SOAP request
* to the JIRA SVN Service that causes it to reindex the repository at the
* specified URL.
*
* @author Justin Garrick
*/
public class JIRASOAPClient {

     /*
     * README:
     * At the time of writing, I am using JIRA 4.1.2. JIRA 4.1.* uses Axis 1.3 internally to dynamically create WSDLs.
     * These generated WSDLs are soul-crushingly awful and invalid.  In fact, they're so bad that they're incompatible
     * with almost every WSDL -> Java tool (Axis2, XFire, etc).  This means that you can either use the old Axis
     * WSDL2Java tool and the terrible stubs that it produces, or you can manually construct the SOAP via some XML stack
     * and send it.  I've chosen the latter, and I'm using the JAX-WS classes instead of Axis2 since they are
     * integrated into the JDK as of 1.6, and as such do not require a third-party lib on the classpath.
     *
     * For more information (why would you want it?) see:
     * http://jira.atlassian.com/browse/JRA-12152
     * http://jira.atlassian.com/browse/JRA-20351
     * http://confluence.atlassian.com/display/JIRA/Creating+a+SOAP+Client
     */

     /**
     * A quick and dirty main method that will send a SOAP request
     * containing the repo URL to the JIRA SVN SOAP service.
     *
     * The outbound SOAP will look like this:
     * <pre>
     * {@code
     * <?xml version="1.0"?>
     * <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
     *   <SOAP-ENV:Body>
     *     <jira:reindexRepository xmlns:jira="http://soap.ext.plugin.jira.atlassian.com">
     *       <in0 xmlns="">
     *         http://yoursvnserver/svn/yourrepo
     *       </in0>
     *     </jira:reindexRepository>
     *   </SOAP-ENV:Body>
     * </SOAP-ENV:Envelope>
     * }
     * </pre> 
     *
     * @param args two strings: the service URL and the repo URL
     * @throws Exception dirty, but we want the commit to work no matter what
     */
     public static void main(final String... args) throws Exception {
          String endpoint = args[0];

          QName port = new QName(endpoint, "subversionsoapservice-v2");
          QName serviceName = new QName(endpoint, "ISubversionSoapServiceService");
         
          Service service = Service.create(serviceName);
          service.addPort(port, SOAPBinding.SOAP11HTTP_BINDING, endpoint);
         
          Dispatch<SOAPMessage> dispatch = service.createDispatch(port, SOAPMessage.class, Service.Mode.MESSAGE);
          MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
          SOAPMessage request = factory.createMessage();
          SOAPBody body = request.getSOAPBody();
         
          SOAPElement reindexRepository = body.addChildElement("reindexRepository", "jira", 
            "http://soap.ext.plugin.jira.atlassian.com");
          SOAPElement in0 = reindexRepository.addChildElement("in0");
          in0.addTextNode(args[1]);
         
          request.saveChanges();
          dispatch.invoke(request);
     }
    
}
```

Compile the class and place it somewhere that your SVN hooks have access to, e.g. C:\HookScripts. Now you can create a post-commit hook that invokes the client, passing it the service URL and the URL of the repository you want to reindex.  Here is an example post-commit hook for Windows (change the values of the variables to suit your installation; obviously this needs to be a shell script for *nix):
```
SET SVC_URL="http://yourjiraserver/rpc/soap/subversionsoapservice-v2"
SET REPO_URL="http://yoursvnserver/svn/yourproject"

SET CLASS_LOC="C:\HookScripts"
SET CLASS_NAME="JIRASOAPClient"
SET JAVA="C:\Program Files\JRE6U23\bin\java.exe"

%JAVA% -cp %CLASS_LOC% %CLASS_NAME% %SVC_URL% %REPO_URL%
```
