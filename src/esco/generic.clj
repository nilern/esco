(ns esco.generic
  (:refer-clojure :exclude [comment])
  (:require [esco.core :refer :all])
  (:import [java.util Iterator]
           [javax.xml.stream.events StartElement Attribute Characters]))

(defrecord Element [tag attrs content])

(declare element)

(def misc (fmap (constantly nil) comment))

(def content
  (plet [ecs (many (orp #'element
                        characters
                        (fmap (constantly nil) comment)))]
    (filter some? ecs)))

(def element
  (plet [^StartElement start start-element
         body content
         _    end-element]
    (->Element (keyword (.. start getName toString))
               (let [^Iterator attrs (.getAttributes start)]
                 (when (.hasNext attrs)
                   (into {}
                         (map (fn [^Attribute attr]
                                [(keyword (.. attr getName toString))
                                 (.getValue attr)]))
		         (iterator-seq attrs))))
               body)))

(def prolog (many misc))

(def document
  (plet [_    start-document
         _    prolog
         body element
         _    end-document]
    body))

