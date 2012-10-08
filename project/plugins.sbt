
resolvers += Resolver.url("Typesafe Releases", new URL("http://repo.typesafe.com/typesafe/releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.5.1")

// uncomment the following 2 lines to authenticate against oss.sonatype.org with sbt 0.12 (not required for publish-local)
// resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

// addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")
