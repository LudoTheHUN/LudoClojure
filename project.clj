(defproject LudoClojure "1.0.0-SNAPSHOT"
  :description "This project is designed to be a practical walk through of all the pain points of writing a Clojure application, hopefully keeping track of aha moments relatd to namespaces, adding dependencies, libs, testing and Clojure specifics and benchmarking"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [calx/javacl "1.0.4b"]
                 [gloss "0.1.1-SNAPSHOT"]
                 ]
                 ;;Note: when creating an UberJar including calx + gloss cause error "Exception in thread "main" java.lang.SecurityException: Invalid signature file digest for Manifest main attributes, need to remove manualy both non manifest files"
  :jvm-opts ["-Xmx1300m"]
  :main LudoClojure.core )