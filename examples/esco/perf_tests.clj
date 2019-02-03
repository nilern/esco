(ns esco.perf-tests
  (:require [clojure.data.xml :as dxml]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [clojure.xml :as xml]
            [criterium.core :refer [bench]]
            [esco.core :as es]
            [esco.generic :as eg])
  (:import [java.io ByteArrayInputStream]
           [javax.xml.stream XMLInputFactory]))

(set! *warn-on-reflection* true)

(def ^:private pom-uri
  "https://raw.githubusercontent.com/apache/maven/master/pom.xml")

(defn- clojure-xml-perf [^bytes xml-bytes]
  (println "# clojure.xml/parse\n")
  (bench (postwalk identity
                   (xml/parse (ByteArrayInputStream. xml-bytes)))))

(defn- clojure-data-xml-perf [^bytes xml-bytes]
  (println "# clojure.data.xml/parse\n")
  ;; clojure.data.xml is lazy but `postwalk` should force things.
  ;; For fairness we also postwalk in every other benchmark.
  (bench (postwalk identity
                   (dxml/parse (ByteArrayInputStream. xml-bytes)))))

(defn- esco-perf [^bytes xml-bytes]
  (println "# esco.core/parse esco.generic/document\n")
  (bench (postwalk identity
                   (es/parse eg/document
                             (.createXMLEventReader (XMLInputFactory/newFactory)
                                                    (ByteArrayInputStream. xml-bytes))))))

(defn -main [& _]
  (let [xml-bytes (.getBytes (slurp pom-uri))]
    ;; Execution time mean : 6,775345 ms
    (clojure-xml-perf xml-bytes)
    
    (println "\n---\n")

    ;; Execution time mean : 5,852482 ms
    (clojure-data-xml-perf xml-bytes)

    (println "\n---\n")

    ;; Execution time mean : 4,492868 ms
    (esco-perf xml-bytes)
    
    0))

