/*
 * Copyright 2006-2008 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.enunciate.modules.amf;

import flex.messaging.MessageBrokerServlet;
import freemarker.ext.dom.NodeModel;
import freemarker.template.*;
import net.sf.jelly.apt.decorations.JavaDoc;
import net.sf.jelly.apt.freemarker.FreemarkerJavaDoc;
import org.apache.commons.digester.RuleSet;
import org.codehaus.enunciate.EnunciateException;
import org.codehaus.enunciate.apt.EnunciateClasspathListener;
import org.codehaus.enunciate.apt.EnunciateFreemarkerModel;
import org.codehaus.enunciate.config.SchemaInfo;
import org.codehaus.enunciate.config.WsdlInfo;
import org.codehaus.enunciate.contract.HasFacets;
import org.codehaus.enunciate.contract.jaxb.TypeDefinition;
import org.codehaus.enunciate.contract.jaxws.EndpointInterface;
import org.codehaus.enunciate.contract.validation.Validator;
import org.codehaus.enunciate.main.*;
import org.codehaus.enunciate.main.webapp.BaseWebAppFragment;
import org.codehaus.enunciate.main.webapp.WebAppComponent;
import org.codehaus.enunciate.modules.FacetAware;
import org.codehaus.enunciate.modules.FlexHomeAwareModule;
import org.codehaus.enunciate.modules.FreemarkerDeploymentModule;
import org.codehaus.enunciate.modules.ProjectExtensionModule;
import org.codehaus.enunciate.modules.amf.config.AMFRuleSet;
import org.codehaus.enunciate.modules.amf.config.FlexApp;
import org.codehaus.enunciate.modules.amf.config.FlexCompilerConfig;
import org.codehaus.enunciate.modules.amf.config.License;
import org.codehaus.enunciate.template.freemarker.AccessorOverridesAnotherMethod;
import org.codehaus.enunciate.template.freemarker.ClientPackageForMethod;
import org.codehaus.enunciate.template.freemarker.ComponentTypeForMethod;
import org.codehaus.enunciate.template.freemarker.SimpleNameWithParamsMethod;
import org.codehaus.enunciate.util.FacetFilter;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * <h1>AMF Module</h1>
 *
 * <p>The AMF deployment module generates the server-side and client-side libraries used to support an
 * <a href="http://en.wikipedia.org/wiki/Action_Message_Format">Action Message Format</a> API. The client-side
 * library is a set of <a href="http://en.wikipedia.org/wiki/Actionscript">ActionScript</a> classes that are
 * type-safe wrappers around the ActionScript remoting API that are designed to add clarity and to be easy
 * to consume for Flex development.  Furthermore, the server-side support classes add an extra degree of security to
 * your Data Services by ensuring that only your public methods are made available for invocation via AMF. There is
 * also support for invoking the <a href="http://en.wikipedia.org/wiki/Adobe_Flex">Adobe Flex</a> compiler to compile
 * a set of <a href="http://en.wikipedia.org/wiki/Adobe_Flash">Flash</a> applications that can be added to your
 * Enunciate-generated web application.</p>
 *
 * <p>The AMF API leverages the <a href="http://labs.adobe.com/technologies/blazeds/">Blaze DS</a> package that was recently made
 * available as an open source product by Adobe. To use the AMF module, you will have to have the
 * <a href="http://www.adobe.com/products/flex/sdk/">Flex SDK</a> installed.</p>
 *
 * <p>This documentation is an overview of how to use Enunciate to build your Flex Data Services (AMF API) and associated Flash
 * application(s). The reader is redirected to the <a href="http://www.adobe.com/products/flex/">documentation for Flex</a> for
 * instructions on how to use Flex. There are also two sample applications you may find useful, petclinic and addressbook, that you
 * will find bundled with the Enunciate distribution.</p>
 *
 * <ul>
 * <li><a href="#steps">steps</a></li>
 * <li><a href="#config">configuration</a></li>
 * <li><a href="#artifacts">artifacts</a></li>
 * </ul>
 *
 * <h1><a name="steps">Steps</a></h1>
 *
 * <h3>generate</h3>
 *
 * <p>The "generate" step generates all source code for the AMF API. This includes server-side support classes and client-side
 * ActionScript classes that can be used to access the API via AMF.</p>
 *
 * <h3>compile</h3>
 *
 * <p>During the "compile" step, the AMF module compiles the code that was generated. The generated client-side ActionScript
 * classes are compiled into an SWC file that is made available as an Enunciate artifact.  The SWC file can also be made
 * available as a download from the deployed web application (see the configuration). It is also during the "compile" step that
 * the Flex compiler is invoked on any Flex applications that are specified in the configuration.</p>
 *
 * <h1><a name="config">Configuration</a></h1>
 *
 * <p>The AMF module is configured by the "amf" element under the "modules" element of the enunciate configuration file.  <b>The
 * AMF module is disabled by default because of the added constraints applied to the service endpoints and because of the additional
 * dependencies required by the module.</b>  To enable AMF, be sure to specify <i>disabled="false"</i> on the "amf" element.</p>
 *
 * <p>The "amf" element supports the following attributes:</p>
 *
 * <ul>
 * <li>The "flexHome" attribute <b>must</b> be supplied. It is the path to the directory where the Flex SDK is installed.</li>
 * <li>The "label" attribute is used to determine the name of the client-side artifact files. The default is the Enunciate project label.</li>
 * <li>The "swcName" attribute specifies the name of the compiled SWC. By default, the name is determined by the Enunciate
 * project label (see the main configuration docs).</li>
 * <li>The "swcDownloadable" attribute specifies whether the generated SWC is to be made available as a download from the
 * generated web application.  Default: "false".</li>
 * <li>The "asSourcesDownloadable" attribute specifies whether the generated ActionScript source files are downloadable from
 * generated web application.  Default: "false".</li>
 * <li>The "mergeServicesConfigXML" attribute specifies the services-config.xml file that is to be merged into the Enunciate-generated 
 * services-config.xml file. No file will be merged if none is specified.</li>
 * <li>The "enforceNoFieldAccessors" attribute specifies whether to enforce that a field accessor cannot be used for AMF mapping.
 * <i>Note: whether this option is enabled or disabled, there currently MUST be a getter and setter for each accessor.  This option only
 * disables the compile-time validation check.</i></li>
 * </ul>
 *
 * <h3>The "war" element</h3>
 *
 * <p>The "war" element under the "amf" element is used to configure the webapp that will host the AMF endpoints and Flex applications. It supports
 * the following attributes:</p>
 *
 * <ul>
 * <li>The "amfSubcontext" attribute is the subcontext at which the amf endpoints will be mounted.  Default: "/amf".</li>
 * <li>The "flexAppDir" attribute is the directory in the war to which the flex applications will be put.  The default is the root of the war.</li>
 * </ul>
 *
 * <h3>The "compiler" element</h3>
 *
 * <p>The "compiler" element under the "amf" element is used to configure the compiler that will be used to compile the SWC
 * and the Flex applications. It supports the following attributes, associated directly to the Flex compiler options.  For details,
 * see the documentation for the Flex compiler.</p>
 *
 * <ul>
 * <li><b>contextRoot</b> (default: the Enunciate project label)</li>
 * <li><b>flexConfig</b> (default: "$FLEX_SDK_HOME/frameworks/flex-config.xml")</li>
 * <li><b>locale</b> (default: unspecified)</li>
 * <li><b>optimize</b> (boolean, default: unspecified)</li>
 * <li><b>debug</b> (boolean, default: unspecified)</li>
 * <li><b>profile</b> (boolean, default: unspecified)</li>
 * <li><b>strict</b> (boolean, default: unspecified)</li>
 * <li><b>useNetwork</b> (boolean, default: unspecified)</li>
 * <li><b>incremental</b> (boolean, default: unspecified)</li>
 * <li><b>warnings</b> (boolean, default: unspecified)</li>
 * <li><b>showActionscriptWarnings</b> (boolean, default: unspecified)</li>
 * <li><b>showBindingWarnings</b> (boolean, default: unspecified)</li>
 * <li><b>showDeprecationWarnings</b> (boolean, default: unspecified)</li>
 * <li><b>flexCompileCommand</b> (default "flex2.tools.Compiler")</li>
 * <li><b>swcCompileCommand</b> (default "flex2.tools.Compc")</li>
 * </ul>
 *
 * <p>The "compiler" element also supports the following subelements:</p>
 *
 * <ul>
 * <li>"JVMArg" (additional JVM arguments, passed in order to the JVM used to invoke the compiler, supports a single attribute: "value")</li>
 * <li>"arg" (additional compiler arguments, passed in order to the compiler)</li>
 * <li>"license" (supports attributes "product" and "serialNumber")</li>
 * </ul>
 *
 * <h3>The "app" element</h3>
 *
 * <p>The AMF module supports the development of Flex apps that can be compiled and packaged with the generated Enunciate app.
 * The "app" element supports the folowing attributes:</p>
 *
 * <ul>
 * <li>The "name" attribute is the name of the Flex app.  This attribute is required.</li>
 * <li>The "srcDir" attribute specifies the source directory for the application. This attribute is required.</li>
 * <li>The "mainMxmlFile" attribute specifies the main mxml file for the app.  This attribute is required. The path to this file is resolved
 * relative to the enunciate.xml file (not to the "srcDir" attribute of the app).</li>
 * <li>The "outputPath" attribute specified the output directory for the application, relative to the "flexAppDir".</li>
 * </ul>
 *
 * <h3>The "facets" element</h3>
 *
 * <p>The "facets" element is applicable to the AMF module to configure which facets are to be included/excluded from the AMF artifacts. For
 * more information, see <a href="http://docs.codehaus.org/display/ENUNCIATE/Enunciate+API+Facets">API Facets</a></p>
 *
 * <h3>Example Configuration</h3>
 *
 * <p>As an example, consider the following configuration:</p>
 *
 * <code class="console">
 * &lt;enunciate&gt;
 * &nbsp;&nbsp;&lt;modules&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;amf disabled="false" swcName="mycompany-amf.swc"
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flexHome="/home/myusername/tools/flex-sdk-2"&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;app srcDir="src/main/flexapp" name="main" mainMxmlFile="src/main/flexapp/com/mycompany/main.mxml"/&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;app srcDir="src/main/anotherapp" name="another" mainMxmlFile="src/main/anotherapp/com/mycompany/another.mxml"/&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;facets&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/facets&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/amf&gt;
 * &nbsp;&nbsp;&lt;/modules&gt;
 * &lt;/enunciate&gt;
 * </code>
 *
 * <p>This configuration enables the AMF module and gives a specific name to the compiled SWC for the client-side ActionScript classes.</p>
 *
 * <p>There also two Flex applications defined. The first is located at "src/main/flexapp". The name of this app is "main". The MXML
 * file that defines this app sits at "src/main/flexapp/com/mycompany/main.mxml", relative to the enunciate configuration file. The
 * second application, rooted at "src/main/anotherapp", is named "another". The mxml file that defines this application sits at
 * "src/main/anotherapp/com/mycompany/another.mxml".</p>
 *
 * <p>After the "compile" step of the AMF module, assuming everything compiles correctly, there will be two Flash applications, "main.swf" and "another.swf",
 * that sit in the applications directory (see "artifacts" below).</p>
 *
 * <p>For a less contrived example, see the "petclinic" sample Enunciate project bundled with the Enunciate distribution.</p>
 *
 * <h1><a name="artifacts">Artifacts</a></h1>
 *
 * <ul>
 * <li>The "amf.client.src.dir" artifact is the directory where the client-side source code is generated.</li>
 * <li>The "amf.server.src.dir" artifact is the directory where the server-side source code is generated.</li>
 * <li>The "as3.client.swc" artifact is the packaged client-side ActionScript SWC.</li>
 * <li>The "flex.app.dir" artifact is the directory to which the Flex apps are compiled.</li>
 * </ul>
 *
 * @author Ryan Heaton
 * @docFileName module_amf.html
 */
public class AMFDeploymentModule extends FreemarkerDeploymentModule implements ProjectExtensionModule, FlexHomeAwareModule, EnunciateClasspathListener, FacetAware {

  private String amfSubcontext = "/amf/";
  private String flexAppDir = null;
  private final List<FlexApp> flexApps = new ArrayList<FlexApp>();
  private final AMFRuleSet configurationRules = new AMFRuleSet();

  private String label = null;
  private String flexHome = System.getProperty("flex.home") == null ? System.getenv("FLEX_HOME") : System.getProperty("flex.home");
  private FlexCompilerConfig compilerConfig = new FlexCompilerConfig();
  private String swcName;
  private boolean swcDownloadable = false;
  private boolean asSourcesDownloadable = false;
  private boolean amfRtFound = false;
  private boolean springDIFound = false;
  private boolean enforceNoFieldAccessors = true;
  private String mergeServicesConfigXML;
  private Set<String> facetIncludes = new TreeSet<String>();
  private Set<String> facetExcludes = new TreeSet<String>(Arrays.asList("org.codehaus.enunciate.modules.amf.AMFTransient"));

  /**
   * @return "amf"
   */
  @Override
  public String getName() {
    return "amf";
  }

  @Override
  public void init(Enunciate enunciate) throws EnunciateException {
    super.init(enunciate);

    if (!isDisabled()) {
      if (this.flexHome == null && (isSwcDownloadable() || !flexApps.isEmpty())) {
        throw new EnunciateException("To compile a flex app you must specify the Flex SDK home directory, either in configuration, by setting the FLEX_HOME environment variable, or setting the 'flex.home' system property.");
      }

      for (FlexApp flexApp : flexApps) {
        if (flexApp.getName() == null) {
          throw new EnunciateException("A flex app must have a name.");
        }

        String srcPath = flexApp.getSrcDir();
        if (srcPath == null) {
          throw new EnunciateException("A source directory for the flex app '" + flexApp.getName() + "' must be supplied with the 'srcDir' attribute.");
        }

        File srcDir = enunciate.resolvePath(srcPath);
        if (!srcDir.exists()) {
          throw new EnunciateException("Source directory for the flex app '" + flexApp.getName() + "' doesn't exist.");
        }
      }
    }
  }

  @Override
  public void initModel(EnunciateFreemarkerModel model) {
    super.initModel(model);

    if (!isDisabled()) {
      if (!amfRtFound) {
        warn("WARNING! The AMF module is enabled, but the Enunciate AMF runtime libraries were found on the Enunciate classpath. " +
          "Application startup will fail unless the Enunciate AMF runtime libraries are on the classpath.");
      }
    }
  }

  public void onClassesFound(Set<String> classes) {
    amfRtFound |= classes.contains("org.codehaus.enunciate.modules.amf.AMFEndpointImpl");
    springDIFound |= classes.contains("org.springframework.beans.factory.annotation.Autowired");
  }

  @Override
  public void doFreemarkerGenerate() throws IOException, TemplateException, EnunciateException {
    File serverGenerateDir = getServerSideGenerateDir();
    File clientGenerateDir = getClientSideGenerateDir();
    File xmlGenerateDir = getXMLGenerateDir();

    Enunciate enunciate = getEnunciate();
    if (!enunciate.isUpToDateWithSources(serverGenerateDir) ||
      !enunciate.isUpToDateWithSources(clientGenerateDir) ||
      !enunciate.isUpToDateWithSources(xmlGenerateDir)) {

      //load the references to the templates....
      URL amfEndpointTemplate = getTemplateURL("amf-endpoint.fmt");
      URL amfTypeTemplate = getTemplateURL("amf-type.fmt");
      URL amfTypeMapperTemplate = getTemplateURL("amf-type-mapper.fmt");

      EnunciateFreemarkerModel model = getModel();
      model.setFileOutputDirectory(serverGenerateDir);
      model.put("useSpringDI", this.springDIFound);

      TreeMap<String, String> packages = new TreeMap<String, String>(new Comparator<String>() {
        public int compare(String package1, String package2) {
          int comparison = package1.length() - package2.length();
          if (comparison == 0) {
            return package1.compareTo(package2);
          }
          return comparison;
        }
      });

      for (SchemaInfo schemaInfo : model.getNamespacesToSchemas().values()) {
        for (TypeDefinition typeDefinition : schemaInfo.getTypeDefinitions()) {
          if (!isFacetExcluded(typeDefinition)) {
            String packageName = typeDefinition.getPackage().getQualifiedName();
            packages.put(packageName, packageName + ".amf");
          }
        }
      }

      debug("Generating the AMF externalizable types and their associated mappers...");
      AMFClassnameForMethod amfClassnameForMethod = new AMFClassnameForMethod(packages);
      model.put("simpleNameFor", new SimpleNameWithParamsMethod(amfClassnameForMethod));
      model.put("classnameFor", amfClassnameForMethod);
      for (SchemaInfo schemaInfo : model.getNamespacesToSchemas().values()) {
        for (TypeDefinition typeDefinition : schemaInfo.getTypeDefinitions()) {
          if (!isFacetExcluded(typeDefinition)) {
            model.put("type", typeDefinition);
            processTemplate(amfTypeTemplate, model);
            processTemplate(amfTypeMapperTemplate, model);
          }
        }
      }

      debug("Generating the AMF endpoint beans...");
      for (WsdlInfo wsdlInfo : model.getNamespacesToWSDLs().values()) {
        for (EndpointInterface ei : wsdlInfo.getEndpointInterfaces()) {
          if (!isFacetExcluded(ei)) {
            model.put("endpointInterface", ei);
            processTemplate(amfEndpointTemplate, model);
          }
        }
      }

      URL endpointTemplate = getTemplateURL("as3-endpoint.fmt");
      URL typeTemplate = getTemplateURL("as3-type.fmt");
      URL enumTypeTemplate = getTemplateURL("as3-enum-type.fmt");

      //build up the list of as3Aliases...
      HashMap<String, String> as3Aliases = new HashMap<String, String>();
      for (SchemaInfo schemaInfo : model.getNamespacesToSchemas().values()) {
        for (TypeDefinition typeDefinition : schemaInfo.getTypeDefinitions()) {
          if (!isFacetExcluded(typeDefinition)) {
            as3Aliases.put(amfClassnameForMethod.convert(typeDefinition), typeDefinition.getClientSimpleName());
          }
        }
      }

      model.setFileOutputDirectory(clientGenerateDir);
      HashMap<String, String> conversions = new HashMap<String, String>();
      //todo: accept client-side package mappings?
      ClientPackageForMethod as3PackageFor = new ClientPackageForMethod(conversions);
      as3PackageFor.setUseClientNameConversions(true);
      model.put("packageFor", as3PackageFor);
      AS3UnqualifiedClassnameForMethod as3ClassnameFor = new AS3UnqualifiedClassnameForMethod(conversions);
      as3ClassnameFor.setUseClientNameConversions(true);
      model.put("classnameFor", as3ClassnameFor);
      model.put("simpleNameFor", new SimpleNameWithParamsMethod(as3ClassnameFor));
      ComponentTypeForMethod as3ComponentTypeFor = new ComponentTypeForMethod(conversions);
      as3ComponentTypeFor.setUseClientNameConversions(true);
      model.put("componentTypeFor", as3ComponentTypeFor);
      model.put("amfClassnameFor", amfClassnameForMethod);
      model.put("amfComponentTypeFor", new ComponentTypeForMethod(packages));
      model.put("forEachAS3Import", new ForEachAS3ImportTransform(null, as3ClassnameFor));
      model.put("accessorOverridesAnother", new AccessorOverridesAnotherMethod());
      model.put("as3Aliases", as3Aliases);

      debug("Generating the ActionScript types...");
      for (SchemaInfo schemaInfo : model.getNamespacesToSchemas().values()) {
        for (TypeDefinition typeDefinition : schemaInfo.getTypeDefinitions()) {
          if (!isFacetExcluded(typeDefinition)) {
            model.put("type", typeDefinition);
            URL template = typeDefinition.isEnum() ? enumTypeTemplate : typeTemplate;
            processTemplate(template, model);
          }
        }
      }

      for (WsdlInfo wsdlInfo : model.getNamespacesToWSDLs().values()) {
        for (EndpointInterface ei : wsdlInfo.getEndpointInterfaces()) {
          if (!isFacetExcluded(ei)) {
            model.put("endpointInterface", ei);
            processTemplate(endpointTemplate, model);
          }
        }
      }

      File servicesConfigXML = new File(xmlGenerateDir, "services-config.xml");
      URL servicesConfigTemplate = getTemplateURL("services-config-xml.fmt");

      model.setFileOutputDirectory(xmlGenerateDir);
      debug("Generating the configuration files.");
      processTemplate(servicesConfigTemplate, model);

      File mergeTarget = new File(xmlGenerateDir, "merged-services-config.xml");
      if (this.mergeServicesConfigXML != null) {
        URL servicesConfigXmlToMerge = enunciate.resolvePath(this.mergeServicesConfigXML).toURL();

        try {
          model.put("source1", loadMergeXmlModel(servicesConfigXmlToMerge.openStream()));
          model.put("source2", loadMergeXmlModel(new FileInputStream(servicesConfigXML)));
          processTemplate(getMergeServicesConfigXmlTemplateURL(), model);
        }
        catch (TemplateException e) {
          throw new EnunciateException("Error while merging services-config xml files.", e);
        }

        debug("Merged %s and %s into %s...", servicesConfigXmlToMerge, servicesConfigXML, mergeTarget);
      }
      else {
        enunciate.copyFile(servicesConfigXML, mergeTarget);
      }
      
      if (!mergeTarget.exists()) {
          throw new EnunciateException("Error: " + mergeTarget + " doesn't exist.");
        }
    }
    else {
      info("Skipping generation of AMF support as everything appears up-to-date...");
    }

    this.enunciate.addArtifact(new FileArtifact(getName(), "amf.client.src.dir", clientGenerateDir));
    this.enunciate.addArtifact(new FileArtifact(getName(), "amf.server.src.dir", serverGenerateDir));

    this.enunciate.addAdditionalSourceRoot(serverGenerateDir);
  }
  
  /**
   * Loads the node model for merging xml.
   *
   * @param inputStream The input stream of the xml.
   * @return The node model.
   */
  protected NodeModel loadMergeXmlModel(InputStream inputStream) throws EnunciateException {
    try {
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(false); //no namespace for the merging...
      Document doc = builderFactory.newDocumentBuilder().parse(inputStream);
      NodeModel.simplify(doc);
      return NodeModel.wrap(doc.getDocumentElement());
    }
    catch (Exception e) {
      throw new EnunciateException("Error parsing web.xml file for merging", e);
    }
  }

  /**
   * Invokes the flex compiler on the apps specified in the configuration file.
   */
  protected void doFlexCompile() throws EnunciateException, IOException {
    File swcFile = null;
    File asSources = null;
    Enunciate enunciate = getEnunciate();
    if (isSwcDownloadable() || !flexApps.isEmpty()) {
      if (this.flexHome == null) {
        throw new EnunciateException("To compile a flex app you must specify the Flex SDK home directory, either in configuration, by setting the FLEX_HOME environment variable, or setting the 'flex.home' system property.");
      }

      File flexHomeDir = new File(this.flexHome);
      if (!flexHomeDir.exists()) {
        throw new EnunciateException("Flex home not found ('" + flexHomeDir.getAbsolutePath() + "').");
      }

      File javaBinDir = new File(System.getProperty("java.home"), "bin");
      File javaExecutable = new File(javaBinDir, "java");
      if (!javaExecutable.exists()) {
        //append the "exe" for windows users.
        javaExecutable = new File(javaBinDir, "java.exe");
      }

      String javaCommand = javaExecutable.getAbsolutePath();
      if (!javaExecutable.exists()) {
        warn("No java executable found in %s.  We'll just hope the environment is set up to execute 'java'...", javaBinDir.getAbsolutePath());
        javaCommand = "java";
      }

      int compileCommandIndex;
      int outputFileIndex;
      int sourcePathIndex;
      int mainMxmlPathIndex;
      List<String> commandLine = new ArrayList<String>();
      int argIndex = 0;
      commandLine.add(argIndex++, javaCommand);
      for (String jvmarg : this.compilerConfig.getJVMArgs()) {
        commandLine.add(argIndex++, jvmarg);
      }
      commandLine.add(argIndex++, "-cp");
      File flexHomeLib = new File(flexHomeDir, "lib");
      if (!flexHomeLib.exists()) {
        throw new EnunciateException("File not found: " + flexHomeLib);
      }
      else {
        StringBuilder builder = new StringBuilder();
        Iterator<File> flexLibIt = Arrays.asList(flexHomeLib.listFiles()).iterator();
        while (flexLibIt.hasNext()) {
          File flexJar = flexLibIt.next();
          if (flexJar.getAbsolutePath().endsWith("jar")) {
            builder.append(flexJar.getAbsolutePath());
            if (flexLibIt.hasNext()) {
              builder.append(File.pathSeparatorChar);
            }
          }
          else {
            debug("File %s will not be included on the classpath because it's not a jar.", flexJar);
          }
        }
        commandLine.add(argIndex++, builder.toString());
      }

      compileCommandIndex = argIndex;
      commandLine.add(argIndex++, null);

      commandLine.add(argIndex++, "-output");
      outputFileIndex = argIndex;
      commandLine.add(argIndex++, null);

      if (compilerConfig.getFlexConfig() == null) {
        compilerConfig.setFlexConfig(new File(new File(flexHome, "frameworks"), "flex-config.xml"));
      }

      if (compilerConfig.getFlexConfig().exists()) {
        commandLine.add(argIndex++, "-load-config");
        commandLine.add(argIndex++, compilerConfig.getFlexConfig().getAbsolutePath());
      }
      else {
        warn("Configured flex configuration file %s doesn't exist.  Ignoring...", compilerConfig.getFlexConfig());
      }

      if (compilerConfig.getContextRoot() == null) {
        if (getEnunciate().getConfig().getLabel() != null) {
          compilerConfig.setContextRoot("/" + getEnunciate().getConfig().getLabel());
        }
        else {
          compilerConfig.setContextRoot("/enunciate");
        }
      }

      commandLine.add(argIndex++, "-compiler.context-root");
      commandLine.add(argIndex++, compilerConfig.getContextRoot());

      if (compilerConfig.getLocale() != null) {
        commandLine.add(argIndex++, "-compiler.locale");
        commandLine.add(argIndex++, compilerConfig.getLocale());
      }

      if (compilerConfig.getLicenses().size() > 0) {
        commandLine.add(argIndex++, "-licenses.license");
        for (License license : compilerConfig.getLicenses()) {
          commandLine.add(argIndex++, license.getProduct());
          commandLine.add(argIndex++, license.getSerialNumber());
        }
      }

      if (compilerConfig.getOptimize() != null && compilerConfig.getOptimize()) {
        commandLine.add(argIndex++, "-compiler.optimize");
      }

      if (compilerConfig.getDebug() != null && compilerConfig.getDebug()) {
        commandLine.add(argIndex++, "-compiler.debug=true");
      }

      if (compilerConfig.getStrict() != null && compilerConfig.getStrict()) {
        commandLine.add(argIndex++, "-compiler.strict");
      }

      if (compilerConfig.getUseNetwork() != null && compilerConfig.getUseNetwork()) {
        commandLine.add(argIndex++, "-use-network");
      }

      if (compilerConfig.getIncremental() != null && compilerConfig.getIncremental()) {
        commandLine.add(argIndex++, "-compiler.incremental");
      }

      if (compilerConfig.getShowActionscriptWarnings() != null && compilerConfig.getShowActionscriptWarnings()) {
        commandLine.add(argIndex++, "-show-actionscript-warnings");
      }

      if (compilerConfig.getShowBindingWarnings() != null && compilerConfig.getShowBindingWarnings()) {
        commandLine.add(argIndex++, "-show-binding-warnings");
      }

      if (compilerConfig.getShowDeprecationWarnings() != null && compilerConfig.getShowDeprecationWarnings()) {
        commandLine.add(argIndex++, "-show-deprecation-warnings");
      }

      for (String arg : this.compilerConfig.getArgs()) {
        commandLine.add(argIndex++, arg);
      }

      commandLine.add(argIndex++, "-compiler.services");
      File xmlGenerateDir = getXMLGenerateDir();
      commandLine.add(argIndex++, new File(xmlGenerateDir, "merged-services-config.xml").getAbsolutePath());

      commandLine.add(argIndex, "-include-sources");
      File clientSideGenerateDir = getClientSideGenerateDir();
      commandLine.add(argIndex + 1, clientSideGenerateDir.getAbsolutePath());

      String swcName = getSwcName();

      if (swcName == null) {
        String label = "enunciate";
        if (getLabel() != null) {
          label = getLabel();
        }
        else if ((enunciate.getConfig() != null) && (enunciate.getConfig().getLabel() != null)) {
          label = enunciate.getConfig().getLabel();
        }

        swcName = label + "-as3-client.swc";
      }

      File swcCompileDir = getSwcCompileDir();
      swcFile = new File(swcCompileDir, swcName);
      boolean swcUpToDate = swcFile.exists() &&
        enunciate.isUpToDate(xmlGenerateDir, swcCompileDir) &&
        enunciate.isUpToDate(clientSideGenerateDir, swcCompileDir);

      if (!swcUpToDate) {
        commandLine.set(compileCommandIndex, compilerConfig.getSwcCompileCommand());
        commandLine.set(outputFileIndex, swcFile.getAbsolutePath());
        debug("Compiling %s for the client-side ActionScript classes...", swcFile.getAbsolutePath());
        if (enunciate.isDebug()) {
          StringBuilder command = new StringBuilder();
          for (String commandPiece : commandLine) {
            command.append(' ').append(commandPiece);
          }
          debug("Executing SWC compile for client-side actionscript with the command: %s", command);
        }
        compileSwc(commandLine);
      }
      else {
        info("Skipping compilation of %s as everything appears up-to-date...", swcFile.getAbsolutePath());
      }

      //swc is compiled
      while (commandLine.size() > argIndex) {
        //remove the compc-specific options...
        commandLine.remove(argIndex);
      }

      if (compilerConfig.getProfile() != null && compilerConfig.getProfile()) {
        commandLine.add(argIndex++, "-compiler.profile");
      }

      if (compilerConfig.getWarnings() != null && compilerConfig.getWarnings()) {
        commandLine.add(argIndex++, "-warnings");
      }

      commandLine.add(argIndex++, "-source-path");
      commandLine.add(argIndex++, clientSideGenerateDir.getAbsolutePath());

      commandLine.add(argIndex++, "-source-path");
      sourcePathIndex = argIndex;
      commandLine.add(argIndex++, null);

      commandLine.add(argIndex++, "--");
      mainMxmlPathIndex = argIndex;
      commandLine.add(argIndex++, null);

      commandLine.set(compileCommandIndex, compilerConfig.getFlexCompileCommand());

      File outputDirectory = getSwfCompileDir();
      debug("Creating output directory: " + outputDirectory);
      outputDirectory.mkdirs();

      for (FlexApp flexApp : flexApps) {
        String mainMxmlPath = flexApp.getMainMxmlFile();
        if (mainMxmlPath == null) {
          throw new EnunciateException("A main MXML file for the flex app '" + flexApp.getName() + "' must be supplied with the 'mainMxmlFile' attribute.");
        }

        File mainMxmlFile = enunciate.resolvePath(mainMxmlPath);
        if (!mainMxmlFile.exists()) {
          throw new EnunciateException("Main MXML file for the flex app '" + flexApp.getName() + "' doesn't exist.");
        }

        File swfDir = outputDirectory;
        if (flexApp.getOutputPath() != null && !"".equals(flexApp.getOutputPath())) {
          swfDir = new File(outputDirectory, flexApp.getOutputPath());
          swfDir.mkdirs();
        }
        File swfFile = new File(swfDir, flexApp.getName() + ".swf");
        File appSrcDir = enunciate.resolvePath(flexApp.getSrcDir());
        String swfFilePath = swfFile.getAbsolutePath();

        boolean swfUpToDate = swfFile.exists()
          && mainMxmlFile.lastModified() < swfFile.lastModified()
          && enunciate.isUpToDate(appSrcDir, swfFile);

        if (!swfUpToDate) {
          commandLine.set(outputFileIndex, swfFilePath);
          commandLine.set(mainMxmlPathIndex, mainMxmlFile.getAbsolutePath());
          commandLine.set(sourcePathIndex, appSrcDir.getAbsolutePath());

          debug("Compiling %s ...", swfFilePath);
          if (enunciate.isDebug()) {
            StringBuilder command = new StringBuilder();
            for (String commandPiece : commandLine) {
              command.append(' ').append(commandPiece);
            }
            debug("Executing flex compile for module %s with the command: %s", flexApp.getName(), command);
          }

          ProcessBuilder processBuilder = new ProcessBuilder(commandLine.toArray(new String[commandLine.size()]));
          processBuilder.directory(getSwfCompileDir());
          processBuilder.redirectErrorStream(true);
          Process process = processBuilder.start();
          BufferedReader procReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line = procReader.readLine();
          while (line != null) {
            info(line);
            line = procReader.readLine();
          }
          int procCode;
          try {
            procCode = process.waitFor();
          }
          catch (InterruptedException e1) {
            throw new EnunciateException("Unexpected inturruption of the Flex compile process.");
          }

          if (procCode != 0) {
            throw new EnunciateException("Flex compile failed for module " + flexApp.getName());
          }
        }
        else {
          info("Skipping compilation of %s as everything appears up-to-date...", swfFilePath);
        }
      }
    }

    if (isAsSourcesDownloadable()) {
      String label = "enunciate";
      if ((enunciate.getConfig() != null) && (enunciate.getConfig().getLabel() != null)) {
        label = enunciate.getConfig().getLabel();
      }

      asSources = new File(new File(getCompileDir(), "src"), label + "-as3-sources.zip");
      enunciate.zip(asSources, getClientSideGenerateDir());
    }

    if (swcFile != null || asSources != null) {
      List<ArtifactDependency> clientDeps = new ArrayList<ArtifactDependency>();
      BaseArtifactDependency as3Dependency = new BaseArtifactDependency();
      as3Dependency.setId("flex-sdk");
      as3Dependency.setArtifactType("zip");
      as3Dependency.setDescription("The flex SDK.");
      as3Dependency.setURL("http://www.adobe.com/products/flex/");
      as3Dependency.setVersion("2.0.1");
      clientDeps.add(as3Dependency);

      ClientLibraryArtifact as3ClientArtifact = new ClientLibraryArtifact(getName(), "as3.client.library", "ActionScript 3 Client Library");
      as3ClientArtifact.setPlatform("Adobe Flex");
      //read in the description from file:
      as3ClientArtifact.setDescription(readResource("library_description.fmt"));
      as3ClientArtifact.setDependencies(clientDeps);

      if (swcFile != null) {
        NamedFileArtifact clientArtifact = new NamedFileArtifact(getName(), "as3.client.swc", swcFile);
        clientArtifact.setDescription("The compiled SWC.");
        clientArtifact.setPublic(false);
        clientArtifact.setArtifactType(ArtifactType.binaries);
        as3ClientArtifact.addArtifact(clientArtifact);
        enunciate.addArtifact(clientArtifact);
      }

      if (asSources != null) {
        NamedFileArtifact clientArtifact = new NamedFileArtifact(getName(), "as3.client.sources", asSources);
        clientArtifact.setDescription("The client-side ActionScript sources.");
        clientArtifact.setPublic(false);
        clientArtifact.setArtifactType(ArtifactType.sources);
        as3ClientArtifact.addArtifact(clientArtifact);
        enunciate.addArtifact(clientArtifact);
      }

      enunciate.addArtifact(as3ClientArtifact);
    }
  }

  /**
   * Compiles the SWC.
   *
   * @param commandLine The command line.
   */
  protected void compileSwc(List<String> commandLine) throws IOException, EnunciateException {
    getSwcCompileDir().mkdirs();
    Process process = new ProcessBuilder(commandLine).directory(getSwcCompileDir()).redirectErrorStream(true).start();
    BufferedReader procReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line = procReader.readLine();
    while (line != null) {
      info(line);
      line = procReader.readLine();
    }
    int procCode;
    try {
      procCode = process.waitFor();
    }
    catch (InterruptedException e1) {
      throw new EnunciateException("Unexpected inturruption of the Flex compile process.");
    }

    if (procCode != 0) {
      throw new EnunciateException("SWC compile failed.");
    }
  }

  @Override
  protected void doCompile() throws EnunciateException, IOException {
    Enunciate enunciate = getEnunciate();

    if (isSwcDownloadable() || this.flexApps.size() > 0 || isAsSourcesDownloadable()) {
      doFlexCompile();
      if (this.flexApps.size() > 0) {
        enunciate.addArtifact(new FileArtifact(getName(), "flex.app.dir", getSwfCompileDir()));
      }
    }
  }

  @Override
  protected void doBuild() throws EnunciateException, IOException {
    //assemble the server-side webapp fragment
    BaseWebAppFragment webAppFragment = new BaseWebAppFragment(getName());

    //base webapp dir...
    File webappDir = new File(getBuildDir(), "webapp");
    webappDir.mkdirs();
    File servicesConfigFile = new File(getXMLGenerateDir(), "merged-services-config.xml");
    if (servicesConfigFile.exists()) {
      getEnunciate().copyFile(servicesConfigFile, new File(new File(new File(webappDir, "WEB-INF"), "flex"), "services-config.xml"));
    }
    else {
      throw new FileNotFoundException("File not found: " + servicesConfigFile.getAbsolutePath());
    }

    File swfCompileDir = getSwfCompileDir();
    if ((this.flexApps.size() > 0) && (swfCompileDir != null) && (swfCompileDir.exists())) {
      File flexAppDir = webappDir;
      if ((getFlexAppDir() != null) && (!"".equals(getFlexAppDir()))) {
        debug("Flex applications will be put into the %s subdirectory of the web application.", getFlexAppDir());
        flexAppDir = new File(webappDir, getFlexAppDir());
      }
      getEnunciate().copyDir(swfCompileDir, flexAppDir);
    }
    else {
      debug("No flex apps were found.");
    }
    webAppFragment.setBaseDir(webappDir);

    //servlets.
    WebAppComponent messageServlet = new WebAppComponent();
    messageServlet.setClassname(MessageBrokerServlet.class.getName());
    messageServlet.setName("AMFMessageServlet");
    TreeSet<String> urlMappings = new TreeSet<String>();
    for (WsdlInfo wsdlInfo : getModel().getNamespacesToWSDLs().values()) {
      for (EndpointInterface ei : wsdlInfo.getEndpointInterfaces()) {
        urlMappings.add(getAmfSubcontext() + ei.getServiceName());
      }
    }
    messageServlet.setUrlMappings(urlMappings);
    TreeMap<String, String> initParams = new TreeMap<String, String>();
    initParams.put("services.configuration.file", "/WEB-INF/flex/services-config.xml");
    initParams.put("flex.write.path", "/WEB-INF/flex");
    messageServlet.setInitParams(initParams);
    webAppFragment.setServlets(Arrays.asList(messageServlet));

    getEnunciate().addWebAppFragment(webAppFragment);
  }

  /**
   * Reads a resource into string form.
   *
   * @param resource The resource to read.
   * @return The string form of the resource.
   */
  protected String readResource(String resource) throws IOException, EnunciateException {
    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("sample_service_method", getModelInternal().findExampleWebMethod());
    model.put("sample_resource", getModelInternal().findExampleResourceMethod());

    URL res = AMFDeploymentModule.class.getResource(resource);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes);
    try {
      processTemplate(res, model, out);
      out.flush();
      bytes.flush();
      return bytes.toString("utf-8");
    }
    catch (TemplateException e) {
      throw new EnunciateException(e);
    }
  }

  /**
   * Whether the given type declaration is AMF-transient.
   *
   * @param declaration The type declaration.
   * @return Whether the given tyep declaration is AMF-transient.
   */
  protected boolean isFacetExcluded(HasFacets declaration) {
    return !FacetFilter.accept(declaration);
  }

  /**
   * Get a template URL for the template of the given name.
   *
   * @param template The specified template.
   * @return The URL to the specified template.
   */
  protected URL getTemplateURL(String template) {
    return AMFDeploymentModule.class.getResource(template);
  }

  /**
   * Get the generate directory for server-side AMF classes.
   *
   * @return The generate directory for server-side AMF classes.
   */
  public File getServerSideGenerateDir() {
    return new File(getGenerateDir(), "server");
  }

  /**
   * Get the generate directory for client-side AMF classes.
   *
   * @return The generate directory for client-side AMF classes.
   */
  public File getClientSideGenerateDir() {
    return new File(getGenerateDir(), "client");
  }

  /**
   * Get the generate directory for XML configuration.
   *
   * @return The generate directory for the XML configuration.
   */
  public File getXMLGenerateDir() {
    return new File(getGenerateDir(), "xml");
  }

  /**
   * The directory for the destination for the SWC.
   *
   * @return The directory for the destination for the SWC.
   */
  public File getSwcCompileDir() {
    return new File(getCompileDir(), "swc");
  }

  /**
   * The directory for the destination for the SWF.
   *
   * @return The directory for the destination for the SWF.
   */
  public File getSwfCompileDir() {
    return new File(getCompileDir(), "swf");
  }

  /**
   * AMF configuration rule set.
   *
   * @return AMF configuration rule set.
   */
  @Override
  public RuleSet getConfigurationRules() {
    return this.configurationRules;
  }

  /**
   * AMF validator.
   *
   * @return AMF validator.
   */
  @Override
  public Validator getValidator() {
    return new AMFValidator( this.enforceNoFieldAccessors );
  }

  @Override
  protected ObjectWrapper getObjectWrapper() {
    return new DefaultObjectWrapper() {
      @Override
      public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj instanceof JavaDoc) {
          return new FreemarkerJavaDoc((JavaDoc) obj);
        }

        return super.wrap(obj);
      }
    };
  }

  /**
   * The amf home directory
   *
   * @return The amf home directory
   */
  public String getFlexHome() {
    return flexHome;
  }

  /**
   * Set the path to the AMF home directory.
   *
   * @param flexHome The amf home directory
   */
  public void setFlexHome(String flexHome) {
    this.flexHome = flexHome;
  }

  /**
   * The amf apps to compile.
   *
   * @return The amf apps to compile.
   */
  public List<FlexApp> getFlexApps() {
    return flexApps;
  }

  /**
   * Adds a flex app to be compiled.
   *
   * @param flexApp The flex app to be compiled.
   */
  public void addFlexApp(FlexApp flexApp) {
    this.flexApps.add(flexApp);
  }

  /**
   * The compiler configuration.
   *
   * @return The compiler configuration.
   */
  public FlexCompilerConfig getCompilerConfig() {
    return compilerConfig;
  }

  /**
   * The compiler configuration.
   *
   * @param compilerConfig The compiler configuration.
   */
  public void setCompilerConfig(FlexCompilerConfig compilerConfig) {
    this.compilerConfig = compilerConfig;
  }
  
  /**
   * @return The URL to "web.xml.fmt"
   */
  protected URL getMergeServicesConfigXmlTemplateURL() {
    return this.getClass().getResource("merge-services-config-xml.fmt");
  }

  /**
   * The name of the swc file.
   *
   * @return The name of the swc file.
   */
  public String getSwcName() {
    return swcName;
  }

  /**
   * The name of the swc file.
   *
   * @param swcName The name of the swc file.
   */
  public void setSwcName(String swcName) {
    this.swcName = swcName;
  }

  /**
   * Whether the swc is downloadable.
   *
   * @return Whether the swc is downloadable.
   */
  public boolean isSwcDownloadable() {
    return swcDownloadable;
  }

  /**
   * Whether the swc is downloadable.
   *
   * @param swcDownloadable Whether the swc is downloadable.
   */
  public void setSwcDownloadable(boolean swcDownloadable) {
    this.swcDownloadable = swcDownloadable;
  }

  /**
   * Whether the generated ActionScript sources are downloadable.
   *
   * @return Whether the generated ActionScript sources are downloadable.
   */
  public boolean isAsSourcesDownloadable() {
    return asSourcesDownloadable;
  }

  /**
   * Whether the generated ActionScript sources are downloadable.
   *
   * @param asSourcesDownloadable Whether the generated ActionScript sources are downloadable.
   */
  public void setAsSourcesDownloadable(boolean asSourcesDownloadable) {
    this.asSourcesDownloadable = asSourcesDownloadable;
  }
  
  /**
   * The services-config.xml file that is to be merged into the Enunciate-generated 
   * services-config.xml file.
   *
   * @return the services-config.xml file that is to be merged into the Enunciate-generated 
   * services-config.xml file.
   */
  public String getMergeServicesConfigXML() {
      return this.mergeServicesConfigXML;
  }
  
  /**
   * Specifies the services-config.xml file that is to be merged into the Enunciate-generated 
   * services-config.xml file.
   *
   * @param mergeServicesConfigXML the services-config.xml file that is to be merged into the 
   * Enunciate-generated services-config.xml file.
   */
  public void setMergeServicesConfigXML(String mergeServicesConfigXML) {
      this.mergeServicesConfigXML = mergeServicesConfigXML;
  }

  /**
   * The amf subcontext.
   *
   * @return The amf subcontext.
   */
  public String getAmfSubcontext() {
    return amfSubcontext;
  }

  /**
   * The amf subcontext.
   *
   * @param amfSubcontext The amf subcontext.
   */
  public void setAmfSubcontext(String amfSubcontext) {
    if (amfSubcontext == null) {
      throw new IllegalArgumentException("The AMF context must not be null.");
    }

    if ("".equals(amfSubcontext)) {
      throw new IllegalArgumentException("The AMF context must not be the emtpy string.");
    }

    if (!amfSubcontext.startsWith("/")) {
      amfSubcontext = "/" + amfSubcontext;
    }

    if (!amfSubcontext.endsWith("/")) {
      amfSubcontext = amfSubcontext + "/";
    }

    this.amfSubcontext = amfSubcontext;
  }

  /**
   * The flex app dir.
   *
   * @return The flex app dir.
   */
  public String getFlexAppDir() {
    return flexAppDir;
  }

  /**
   * The flex app dir.
   *
   * @param flexAppDir The flex app dir.
   */
  public void setFlexAppDir(String flexAppDir) {
    this.flexAppDir = flexAppDir;
  }

  /**
   * The label for the ActionScript API.
   *
   * @return The label for the ActionScript API.
   */
  public String getLabel() {
    return label;
  }

  /**
   * The label for the ActionScript API.
   *
   * @param label The label for the ActionScript API.
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * The set of facets to include.
   *
   * @return The set of facets to include.
   */
  public Set<String> getFacetIncludes() {
    return facetIncludes;
  }

  /**
   * Add a facet include.
   *
   * @param name The name.
   */
  public void addFacetInclude(String name) {
    if (name != null) {
      this.facetIncludes.add(name);
    }
  }

  /**
   * The set of facets to exclude.
   *
   * @return The set of facets to exclude.
   */
  public Set<String> getFacetExcludes() {
    return facetExcludes;
  }

  /**
   * Add a facet exclude.
   *
   * @param name The name.
   */
  public void addFacetExclude(String name) {
    if (name != null) {
      this.facetExcludes.add(name);
    }
  }

  // Inherited.
  @Override
  public boolean isDisabled() {
    if (super.isDisabled()) {
      return true;
    }
    else if (getModelInternal() != null && getModelInternal().getNamespacesToWSDLs().isEmpty() && getModelInternal().getNamespacesToSchemas().isEmpty()) {
      debug("AMF module is disabled because there are no endpoint interfaces nor any schema objects.");
      return true;
    }

    return false;
  }

  public List<File> getProjectSources() {
    List<File> sources = new ArrayList<File>(Arrays.asList(getClientSideGenerateDir(), getServerSideGenerateDir()));
    for (FlexApp flexApp : getFlexApps()) {
      sources.add(getEnunciate().resolvePath(flexApp.getSrcDir()));
    }
    return sources;
  }

  public List<File> getProjectTestSources() {
    return Collections.emptyList();
  }

  public List<File> getProjectResourceDirectories() {
    return Collections.emptyList();
  }

  public List<File> getProjectTestResourceDirectories() {
    return Collections.emptyList();
  }

  /**
   * Whether to enforce that field accessors can't be used.
   *
   * @return Whether to enforce that field accessors can't be used.
   */
  public boolean isEnforceNoFieldAccessors() {
    return enforceNoFieldAccessors;
  }

  /**
   * Whether to enforce that field accessors can't be used.
   *
   * @param enforceNoFieldAccessors Whether to enforce that field accessors can't be used.
   */
  public void setEnforceNoFieldAccessors(boolean enforceNoFieldAccessors) {
    this.enforceNoFieldAccessors = enforceNoFieldAccessors;
  }


}
