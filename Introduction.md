When I work with an JAXB object graph, I always frustrated.<br>
I expect more fluent and user friendly member access API than JAXB provides me.<br>
I will try to illustrate what I am talking about in a simple example.<br>

In the following I want to create a new maven project descriptor with JAXB generated objects.<br>
<pre><code>Project project = factory.createProject();<br>
<br>
project.setmodelVersion("4.0.0");<br>
project.setGroupId("redmosquito")<br>
project.setArtifactId("jaxb-fluent-api-ext")<br>
project.setPackaging("jar")<br>
project.setVersion("0.0.1")<br>
project.setName("JAXB FLuent API Extensions");<br>
<br>
Dependency dependency = factory.createDependency();<br>
dependency.setGroupId("org.jvnet.jaxb2_commons");<br>
dependency.setArtifactId("jaxb-xjc");<br>
dependency.setVersion("2.1.10");<br>
<br>
Dependencies dependencies = factory.createDependencies();<br>
dependencies.getDependency().add(dependency);<br>
<br>
project.setDependencies(dependencies);<br>
</code></pre>
Code is not really fluent, isn't it?<br>
The use of <code>ObjectFactory</code> and setters to create path are really boring me.<br>
<br>
With the XJC Fluent API Plugin code looks nicer.<br>
<pre><code>Project project = factory.createProject()<br>
  .withModelVersion("4.0.0");<br>
  .withGroupId("redmosquito")<br>
  .withArtifactId("jaxb-fluent-api-ext")<br>
  .withPackaging("jar")<br>
  .withVersion("0.0.1")<br>
  .withName("JAXB FLuent API Extensions");<br>
  .withDependencies(<br>
    factory.createDependencies()<br>
      .withDependency(<br>
        factory.createDependency()<br>
          .withGroupId("org.jvnet.jaxb2_commons")<br>
          .withArtifactId("jaxb-xjc")<br>
          .withVersion("2.1.10")<br>
      )<br>
  );<br>
</code></pre>
Code is more fluent... but not enough yet :o)<br>
<pre><code>Project project = factory.createProject()<br>
  .withModelVersion("4.0.0");<br>
  .withGroupId("redmosquito")<br>
  .withArtifactId("jaxb-fluent-api-ext")<br>
  .withPackaging("jar")<br>
  .withVersion("0.0.1")<br>
  .withName("JAXB FLuent API Extensions");<br>
<br>
project<br>
  .withDependencies() <br>
    .withNewDependency()<br>
      .withGroupId("org.jvnet.jaxb2_commons");<br>
      .withArtifactId("jaxb-xjc");<br>
      .withVersion("2.1.10");<br>
</code></pre>
With the two new methods <code>withDependencies</code> and <code>withNewDependency</code> the job sounds quite simple and fluent.<br>
<ul><li><code>withDependencies</code> creates and sets a new dependencies object to the project object.<br>
</li><li><code>withNewDependency</code> creates and adds a new dependency object to the dependency list of the dependencies object.</li></ul>

This is what the XJC Fluent API Extensions generates for you.<br>
<br>
Now imaging in an other part of code, you have to add new dependency to the project.<br>
Locally you don't know if any dependencies are already registered.<br>
<br>
With no plugin...<br>
<pre><code>void addFooBarDependency(Project project) {<br>
  Dependencies dependencies = project.getDependencies();<br>
<br>
  if (dependencies != null) {<br>
    dependencies = factory.createDependencies();<br>
    project.setDependencies(dependencies);<br>
  }<br>
<br>
  Dependency dependency = factory.createDependency();<br>
  dependency.setGroupId("foo");<br>
  dependency.setArtifactId("bar");<br>
  dependency.setVersion("2.1.10");<br>
<br>
  dependencies.getDependency().add(dependency);<br>
}<br>
</code></pre>
With XJC Fluent API Plugin, the code becomes:<br>
<pre><code>void addFooBarDependency(Project project) {<br>
  Dependencies = project.getDependencies();<br>
<br>
  if (dependencies != null) {<br>
    dependencies = factory.createDependencies();<br>
    project.setDependencies(dependencies);<br>
  }<br>
<br>
  dependencies<br>
    .withDependency(<br>
      factory.createDependency()<br>
        .withGroupId("foo")<br>
        .withArtifactId("bar")<br>
        .withVersion("1.2.3")<br>
    );<br>
}<br>
</code></pre>
And with the XJC Fluent API Extensions...<br>
<pre><code>void addFooBarDependency(Project project) {<br>
  project<br>
    .withDependencies() <br>
      .withNewDependency()<br>
        .withGroupId("foo")<br>
        .withArtifactId("bar")<br>
        .withVersion("1.2.3");<br>
}<br>
</code></pre>

Notice that <code>withDependencies</code> method creates and set a new instance of <code>Dependencies</code> if and only if the current dependencies property is not set.