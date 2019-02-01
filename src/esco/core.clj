(ns esco.core
  (:import [javax.xml.stream XMLEventReader]
           [javax.xml.stream.events XMLEvent]))

(defprotocol EksMLParser
  (probe [self ^XMLEvent xml])
  (parse [self ^XMLEventReader xml]))

(deftype PureParser [v]
  EksMLParser
  (probe [_ _] :nonconsuming)
  (parse [_ _] v))

(def pure ->PureParser)

(deftype FunctorParser [f inner]
  EksMLParser
  (probe [_ event] (probe inner event))
  (parse [_ xml] (f (parse inner xml))))

(def fmap ->FunctorParser)

(deftype ApplicativeParser [fp inner]
  EksMLParser
  (probe [_ event] (let [probed (probe fp event)]
                     (case probed
                       :nonconsuming (probe inner event)
                       probed)))
                     
  (parse [_ xml] (let [f (parse fp xml)]
                   (f (parse inner xml)))))

(def fapply ->ApplicativeParser)

(deftype AlternativeParser [p p*]
  EksMLParser
  (probe [_ event] (let [probed (probe p event)]
                     (case probed
                       :fail (probe p* event)
                       probed)))

  (parse [_ xml] (let [event (.peek ^XMLEventReader xml)]
                   (case (probe p event)
                     :fail (parse p* xml)
                     (parse p xml)))))

(def alt ->AlternativeParser)

(deftype SatParser [pred]
  EksMLParser
  (probe [_ event] (if (pred event) :consuming :fail))
  (parse [_ xml] (.nextEvent ^XMLEventReader xml)))

(def sat ->SatParser)

