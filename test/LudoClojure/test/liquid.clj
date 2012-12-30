(ns LudoClojure.test.liquid
  (:use [LudoClojure.liquid])
  (:use [clojure.test])
  )


(deftest liquid_tests1
  (is (= connections 500)))


(quote
(make_liquid {:liquidsize 32})
(utils/random_liquid_seedZ 343 0.0)

(:flipper myliquid)
  (keys myliquid)
@(lg_enqueue-read 
  (lg_wrap (:context @cl-utils/opencl_env) (utils/random_conectivity_seedZ 4) :int32-le) (:queue @cl-utils/opencl_env))
  


; (def myliquid (make_liquid {:liquidsize (* 64 64 1) :connections 13}))

(def myliquid (make_liquid {:liquidsize (* 64 64 64) :connections 13}))
(:liquidsize myliquid)

(time (do (time (dotimes [n 100] (flop myliquid)))  ;(lg_finish ((:liquid_queue myliquid) @cl-utils/opencl_env))
          (time (count (filter  (fn [x] (>= x 10.0)) (readoff_speced myliquid [0 1000]))) )
        ))

(time (do (dotimes [n 300000] 
      (let [liquidstate (readoff_speced myliquid [0 1000])]
        (do 
           ;(println n ":" (count (filter  (fn [x] (>= x 10.0)) liquidstate)) (take 6 liquidstate) (take 6 (reverse (sort (filter  (fn [x] (> x 6.0)) liquidstate))))  (take 6 (sort (filter  (fn [x] (< x -5.0)) liquidstate)))))
           (println n ":" (count (filter  (fn [x] (>= x 10.0)) liquidstate)) 
                          (count (filter  (fn [x] (>= x 8.0)) liquidstate))
                          (count (filter  (fn [x] (>= x 4.0)) liquidstate))
                          (count (filter  (fn [x] (>= x 2.0)) liquidstate))
                          (count (filter  (fn [x] (>= x 1.0)) liquidstate))
                          (count (filter  (fn [x] (>= x 0.0)) liquidstate))
                          (count (filter  (fn [x] (>= x -1.0)) liquidstate))
                          (count (filter  (fn [x] (>= x -2.0)) liquidstate))
                          (count (filter  (fn [x] (>= x -4.0)) liquidstate))
                          (count (filter  (fn [x] (>= x -8.0)) liquidstate))
                          (count (filter  (fn [x] (>= x -16.0)) liquidstate))
                          (take 10 (filter  (fn [x] (>= x -16.0)) liquidstate))
                          ))
           ;(lg_finish ((:liquid_queue myliquid) @cl-utils/opencl_env))
            (flop myliquid)
           ; (lg_finish ((:liquid_queue myliquid) @cl-utils/opencl_env))
          ;;  (println n ":" @(lg_enqueue-read (:debug_infobuff_buf myliquid) (:queue @cl-utils/opencl_env)))
                     ))))

(* 64 64 64 6 13 100)

;;(time (do (dotimes [n 100] (flop myliquid))  (lg_finish ((:liquid_queue myliquid) @cl-utils/opencl_env))))


(:flipper myliquid)
(flip myliquid)
(:flipper myliquid)

(time (count (time (readoff myliquid :foo))))
(reduce + (readoff myliquid :foo))
(count (filter  (fn [x] (> x 0.0)) (readoff_speced myliquid [0 20])))

(take 20 (filter  (fn [x] (> x 0.0)) (readoff_speced myliquid [0 20])))



(do (dotimes [n 30] 
        (do (flop myliquid)
           (println n ":" (count (filter  (fn [x] (> x 0.1)) (readoff myliquid :foo))) (take 10 (readoff myliquid :foo)) ))))

(flop myliquid)

)

;(+ 2 2)
;(liquid_tests1)

