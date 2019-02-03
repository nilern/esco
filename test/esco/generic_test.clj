(ns esco.generic-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clojure.xml :as xml]
            [esco.core :as es]
            [esco.generic :as eg])
  (:import [javax.xml.stream XMLInputFactory]))

(def ^:private pom-uri
  "https://raw.githubusercontent.com/apache/maven/master/pom.xml")

(def ^:private xml-inputs (XMLInputFactory/newFactory))

(deftest pom-test
  (is (= (let [xes (.createXMLEventReader xml-inputs (io/reader pom-uri))]
           (es/parse eg/document xes))
         (xml/parse pom-uri))))

