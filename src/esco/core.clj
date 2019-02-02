(ns esco.core
  (:import [clojure.lang Var]
           [javax.xml.stream XMLEventReader XMLStreamException]
           [javax.xml.stream.events XMLEvent]))

(defprotocol EksMLParser
  (probe [self ^XMLEvent xml])
  (parse [self ^XMLEventReader xml]))

(deftype FailParser [^String msg]
  EksMLParser
  (probe [_ _] :fail)
  (parse [_ xml]
    (throw (if-some [ev (.peek ^XMLEventReader xml)]
             (XMLStreamException. msg (.getLocation ev))
             (XMLStreamException. msg)))))

(def fail ->FailParser)

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

(defmacro plet [bindings & body]
  (letfn [(body-fn [binders]
            (if (empty? binders)
              `(do ~@body)
              `(fn [~(first binders)]
                 ~(body-fn (rest binders)))))]
    (let [binders (take-nth 2 bindings)
          actions (take-nth 2 (rest bindings))]
      (assert (= (count binders) (count actions)))
      (if (empty? binders)
        `(do ~@body)
        (reduce (fn [acc action] `(fapply ~acc ~action))
                `(fmap ~(body-fn binders) ~(first actions))
                (rest actions))))))

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

(defmacro orp [& alts]
  (if (empty? alts)
    `(fail "orp: out of options")
    `(alt ~(first alts) (orp ~@(rest alts)))))

(deftype SatParser [pred]
  EksMLParser
  (probe [_ event] (if (pred event) :consuming :fail))
  (parse [_ xml] (let [event (.nextEvent ^XMLEventReader xml)]
                   (if (pred event)
                     event
                     (throw (if event
                              (XMLStreamException. (str "Unsatisfied: \"" event \")
                                                   (.getLocation event))
                              (XMLStreamException. (str "Unsatisfied: \"" event \"))))))))

(def sat ->SatParser)

(def start-document (sat #(.isStartDocument ^XMLEvent %)))
(def end-document (sat #(.isEndDocument ^XMLEvent %)))
(def start-element (sat #(.isStartElement ^XMLEvent %)))
(def end-element (sat #(.isEndElement ^XMLEvent %)))
(def characters (sat #(.isCharacters ^XMLEvent %)))

(extend-protocol EksMLParser
  Var
  (probe [self event] (probe @self event))
  (parse [self xml] (parse @self xml)))

(defn opt [parser] (alt parser (pure nil)))

(deftype ManyParser [inner]
  EksMLParser
  (probe [_ event] (let [probed (probe inner event)]
                     (case probed
                       :fail :nonconsuming
                       probed)))
  
  (parse [_ xml] (loop [vs []]
                   (case (probe inner (.peek ^XMLEventReader xml))
                     :fail vs
                     (recur (conj vs (parse inner xml)))))))

(def many ->ManyParser)

