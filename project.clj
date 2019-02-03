(defproject esco "0.1.0-SNAPSHOT"
  :description "XML parser cominators for Clojure and StAX"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[criterium "0.4.4"]
                                  [org.clojure/data.xml "0.0.8"]]}
             :examples {:source-paths #{"examples"}}}
  :aliases {"perf-test" ["with-profiles" "default,dev,examples"
                         "run" "-m" "esco.perf-tests"]}
  :repl-options {:init-ns esco.core})

