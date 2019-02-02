(ns esco.generic
  (:require [esco.core :refer :all])
  (:import [java.util Iterator]
           [javax.xml.stream.events StartElement Attribute Characters]))

(defrecord Element [tag attrs content])

(declare element)

(def content
  (plet [^Characters cs (opt characters)
         ecs (many (plet [el #'element
                          ^Characters cs (opt characters)]
                     [el (some-> cs .getData)]))]
    (filter some? (cons (some-> cs .getData)
                        (mapcat identity ecs)))))

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

(def document
  (plet [_    start-document
         body element
         _    end-document]
    body))

