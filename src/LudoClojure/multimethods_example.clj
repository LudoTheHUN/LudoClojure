(ns LudoClojure.multimethods-example
   (:import (java.util Date Calendar GregorianCalendar)
	   (java.sql Timestamp)
	   ;(org.joda.time DateTime DateTime$Property DateTimeZone 
           ;               Minutes Hours Period Interval)
	  ; (org.joda.time.format ISODateTimeFormat DateTimeFormatter))
  ))


;;Working through
;;https://github.com/francoisdevlin/Full-Disclojure/blob/master/src/episode_007/episode_007.clj
;;http://vimeo.com/8801325

;;----------------------
;; Dispatch Fn
;;----------------------
(defn- to-ms-dispatch
  [& params]
  (let [lead-param (first params)]
    (cond
     (empty? params) ::empty
     (nil? lead-param) ::nil
     true (class lead-param))))


(to-ms-dispatch 23)
(to-ms-dispatch )
(to-ms-dispatch nil)
(to-ms-dispatch (Date. ))
(to-ms-dispatch (Date. (- 2012 1900) 03 01 11 12 13))

;;Point is, whatever the function returns, it will drive how the multimethod will act

(defmulti to-ms to-ms-dispatch)   ;;defines to-ms multimethod, that will inspace what comes in via the dispatch function, and execute whichever implementation is appropriate





(defmethod to-ms Long
  [& params]
  (first params))

(defmethod to-ms Long
  [l]
  l)

(defmethod to-ms Calendar
  [c]
  (to-ms (.getTime c)))


(defmethod to-ms Date
  [d]
  (.getTime d))

(defmethod to-ms Timestamp
  [ts]
  (.getTime ts))

(defmethod to-ms ::empty
  [& params]
  (to-ms (Date. )))

(defmethod to-ms ::nil
  [& params]
  nil)
;;----------------------
;; Convert to Type
;;----------------------
(defn date [& params]
  (Date. (apply to-ms params)))

(defn greg-cal [& params]
  (doto (GregorianCalendar. )
    (.setTime (apply date params))))

(defn sql-ts [& params]
  (Timestamp. (apply to-ms params)))

;;---------------------
;; Usages
;;---------------------
(defn compare-time
  [a b]
  (.compareTo (date a) (date b)))

(defn before?
  "Tests to determine if time a is before time b"
  [a b]
  (= (compare-time a b) -1))

(defn after?
  "Tests to determine if time a is after time b"
  [a b]
  (= (compare-time a b) 1))

(quote
(defn some-db-fn
  [t & other-stuff]
  (update-db {:updated-at (sql-ts t)
	      :more-stuff other-stuff}))
)

;;---------------------
;; Extend
;;---------------------
(quote
(defmethod to-ms DateTime
  [& params]
  (.getMillis (first params)))

(defn joda [& params]
  (DateTime. (apply to-ms params)))
)

(derive clojure.lang.PersistentArrayMap ::map)
(defmethod to-ms ::map
  [& params]
  (let [default-map {:year 2000
		     :month 1
		     :day 1
		     :hour 0
		     :minute 0
		     :second 0
		     :ms 0}
	input-map (first params)
	resulting-map (merge default-map input-map)
	[y mo d h mi s ms] ((juxt :year :month :day :hour :minute :second :ms)
			    resulting-map)]
  ;;  (to-ms (+ y mo d h mi s ms))))
    (to-ms (Date. y mo d h mi s))))

(isa? clojure.lang.PersistentArrayMap ::map)

(to-ms {:year  2023})

(quote  (to-ms [123])
        )


(quote
(defn- to-ms-dispatch
  [& params]
  (let [lead-param (first params)]
    (cond
     (empty? params) ::empty
     (nil? lead-param) ::nil
     (= lead-param ::map) ::map
     true (class lead-param))))

)



;----------
;Two argument multimethod
;----------
mrfoowenttotownoneday
(defn- two_param_dispacher
  [& params]
  (let [lead-param (first params)
        second-param (second params)]
    (cond
     (= lead-param second-param) ::same
     (empty? params) ::empty
     (nil? lead-param) ::nil
     true ::other)))


(defmulti act-when-same two_param_dispacher)

(defmethod act-when-same ::same [a b]
  (str "first  " a " then " b " again"))

(defmethod act-when-same ::other [a b]
  (str "first  " a " then a different " b ))

(= (two_param_dispacher 1 2)  ::other2)

(act-when-same 1 2)
(act-when-same "1" "1")
(act-when-same 2 2)
  
  
(to-ms-dispatch 23)
(to-ms-dispatch )
(to-ms-dispatch nil)
(to-ms-dispatch (Date. ))
(to-ms-dispatch (Date. (- 2012 1900) 03 01 11 12 13))

;;Point is, whatever the function returns, it will drive how the multimethod will act

   ;;defines to-ms multimethod, that will inspace what comes in via the dispatch function, and execute whichever implementation is appropriate

