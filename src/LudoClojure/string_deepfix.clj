(ns LudoClojure.string-deepfix
(require [clojure.contrib.str-utils2 :as s])
(require [clojure.contrib.json :as j])
  )

(def customvalue "1,231.000")

;using http://clojure.github.com/clojure-contrib/str-utils2-api.html#clojure.contrib.str-utils2/replace

;(defn str2int [txt]
;  (Integer/parseInt (s/replace txt #"[a-zA-Z.]" "")))

;(defn WIP_str2float [txt]
;  (s/replace txt #"[a-zA-Z]" ""))

(defn first-match [m]
  (if (coll? m) (first m) m))

(defn match [regex text]
  (let [m (first-match (re-find (re-pattern regex) text))]
    (if (nil? m)
      [0 0]
      (let [ind (.indexOf text m) len (.length m)]
 [ind (+ ind len)]))))


(match  #"[a-zA-Z]."   customvalue)
(re-find #"[a-zA-Z.]" customvalue)

;;Can use these in combination to figure out what you are looking at...
(re-find #"[.]" customvalue)
(s/replace customvalue #"[a-zA-Z.]" "")
(match  #"[.]"   customvalue)



(defn customvalue_inspector [customvalue]   ;;TODO consider making this a number only inspector...
(cond
;;Good integer check
   (= (count (s/replace customvalue #"[0-9]" "")) 0)   ;count of chars after all numbers are removed needs to be zero to have an int
   :integer

;;Good non integer number check
  (and
       (= (re-find #"[.]" customvalue) ".")   ;there is a dot
       (= (count (s/replace customvalue #"[0-9.]" "")) 0))    ;number of chars that are left after removing number and dot is zero
     :clean_nonint_number    ;meaning a number with like 2342342.00  but not an integer

;;Good check for a clean float
  (and
       (= (re-find #"[.]" customvalue) ".")   ;there is a dot
       (= (count (s/replace customvalue #"[0-9.]" "")) 0))    ;number of chars that are left after removing number and dot is zero
     :clean_nonint_number    ;meaning a number with like 2342342.00  but not an integer


;;Good check for a thousands delimited number 
  (and
       (= (count (re-find #"[.].*" customvalue)) 3)    ;are there just two values after the dot
       (= (count (s/replace (re-find #"[.].*" customvalue) #"[0-9.]" "")) 0)  ;the chars after dot are numbers
       (or (= (count (re-find #"[,].*[.]" customvalue)) 5  )
           (= (count (re-find #"[,].*[.]" customvalue)) 9))   ;there is 3 or 6 digits between commas
       (= (count (s/replace customvalue #"[0-9.,]" "")) 0)    ;there are zero no digits in the number
       )
      :clean_comaatthousands_number

;;Good check for a reverse convention thousands delimited number
  (and
       (= (count (re-find #"[,].*" customvalue)) 3)    ;are there just two values after the dot
       (= (count (s/replace (re-find #"[,].*" customvalue) #"[0-9,]" "")) 0)  ;the chars after dot are numbers
       (or (= (count (re-find #"[.].*[,]" customvalue)) 5  )
           (= (count (re-find #"[.].*[,]" customvalue)) 9))   ;there is 3 or 6 digits between commas
       (= (count (s/replace customvalue #"[0-9.,]" "")) 0)    ;there are zero no digits in the number
       )
      :clean_reverseconvention_number
)   ;;End cond
)

(quote   ;TODO
    (def customvalue "£31.0")  ;;curency first inspector + fixer
    (def customvalue "31.0£")  ;;curency last inspector + fixer
)

;;    (customvalue_inspector customvalue)

;;;; Here we clean up as best we can:
;;TODO   have a clean up strategy for each result type

(defn cleanup__clean_comaatthousands_number [customvalue]
           (s/replace customvalue #"[,]" ""))

(defn cleanup__clean_reverseconvention_number [customvalue]
           (s/replace
             (s/replace customvalue #"[.]" "") #"[,]" "."))



(def customvalue "31.0")
     ;;TODO make this a number only cleaner
(case
 (customvalue_inspector customvalue)
  :clean_comaatthousands_number    (cleanup__clean_comaatthousands_number customvalue)
  :clean_reverseconvention_number           (cleanup__clean_reverseconvention_number customvalue)
  :someotherdetection        "no foo"
  customvalue       ;;just return whatever you were looking up if you can't fix it...
)


     
;;Json work here:  http://richhickey.github.com/clojure-contrib/json-api.html
(j/json-str "foo")
(println (j/json-str {:foo "34"}))

;(cleanup__clean_reverseconvention_number customvalue)

;;TODO dispatch function based on test results





           (s/replace customvalue #"[0-9.]" "")
           ;number of chars that are left after removing number and dot is zero
     :clean_nonint_number    ;meaning a number with like 2342342.00  but not an integer





(defn test_number [customvalue]

(s/contains? "fop" "o")    ; -> true

(s/contains? "[dff" "[")   ; -> true


