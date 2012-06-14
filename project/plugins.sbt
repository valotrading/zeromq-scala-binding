resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases")) (Resolver.ivyStylePatterns)

// comment this out unless you want to authenticate against oss.sonatype.org (not required for publish-local)
//addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6.1")

addSbtPlugin("org.smop" % "javadoc-sbt" % "0.1.0-SNAPSHOT")

