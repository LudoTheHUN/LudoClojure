(ns LudoClojure.test.pperceptron
  (:use [LudoClojure.opencl-utils])
  (:use [LudoClojure.pperceptron])
  (:use [LudoClojure.pperceptron-testhelpers])
  (:use [LudoClojure.utils])
  (:use [clojure.test])
  (:use [calx])
  )

;;(use 'calx)
(def testdata (make_test_array 20 40 3))


(deftest test_manual_pp_test_pp0


(def pp0 (make_pp {:input_size 5
                   :outputs_size 3
                   :pp_size 3
                   :rho 1                    ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 0.9 )}))

(pp_readout pp0 :input_data_buf)
(pp_readout pp0 :correct_answer_buf)
(pp_readout pp0 :pp_answer_buf)
(pp_readout pp0 :vecProductResult_buf)

(let  [input_data [-1.0 1.0 1.0 1.0 -1.0]]
  (pp_write_input pp0 input_data)
  (is (= (map float input_data) (pp_readout pp0 :input_data_buf))))

(let  [input_data [0.333 1.0 1.0 1.0 -1.0]]
  (pp_write_input pp0 input_data)
  (is (= (map float input_data) (pp_readout pp0 :input_data_buf))))

(let  [input_data [0.333434343434343434343434343433 1.0 1.0 1.0 -1.0]]
  (pp_write_input pp0 input_data)
  (is (= (map float input_data) (pp_readout pp0 :input_data_buf))))

  (class (pp_readout pp0 :input_data_buf))
  ;(class input_data)
  (class (first (pp_readout pp0 :input_data_buf)))
  ;(class (first input_data))

(let [input_data [1.9 1.0 1.0 1.0 -1.9]
      buf        (lg_wrap (:context @opencl_env) (map float input_data) :float32-le)]
   (pp_write_input pp0 buf)
   (is (= (map float input_data) (pp_readout pp0 :input_data_buf)))
   )

(let [out_data [1.0 1.0 1.0]
      buf        (lg_wrap (:context @opencl_env) (map float out_data) :float32-le)]
   (pp_write_correct_answer pp0 buf)
   (is (= (map float out_data) (pp_readout pp0 :correct_answer_buf)))
   )




)


  





;;  (test_manual_pp_test_pp0)
  

(deftest test_manual_pp_test_pp1

(with-out-str (do 
(def pp1 (make_pp {:input_size 5     ;;ERROR frist half of output is incorrect
                   
                   :outputs_size 22
                   :pp_size 30
                   :rho 20                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.001)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 1.0 )}))

(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))

(pp_readout pp1 :input_data_buf)
(pp_readout pp1 :correct_answer_buf)
(pp_readout pp1 :pp_answer_buf)
(pp_readout pp1 :vecProductResult_buf)
;(count 
  ;(pp_readout pp1 :alpha_buf)
  ;)
  (pp_train_and_answer pp1 [-1.0 1.0 1.0 1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.001)  })
  (pp_readout pp1 :correct_answer_buf)
  (count     (pp_readout pp1 :alpha_buf)    )

(time (dotimes [n 350]
  (if (= 0 (mod n 1))
    
    (do
       
;      (Thread/sleep 10)
 (lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
  (println n)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 1.0 1.0 1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 0.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 0.0 -1.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " " 
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 1.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
  (pp_train_and_answer pp1 [-1.0 -1.0 0.0 -1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
  )))
(lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
; (Thread/sleep 1)
(print " "
  ;(take 10 (pp_readout pp1 :alpha_buf))
  (if (= 0 (mod n 10))
     (sort (frequencies   (map (fn [x] 
                                 (if  (< x -666.6)
                                   (throw (throw (Exception. "wtf")))
                                 (/ (int (* x 10.0)) 10.0)
                                 )
                                 ) (pp_readout pp1 :alpha_buf) )))
     
     ;(sort (frequencies   (map (fn [x] (/ (int (* x 1000.0)) 1000.0)) (pp_readout pp1 :alpha_buf) )))
  )
  (lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))
  ;(pp_readout pp1 :vecProductResult_buf)
  ;(pp_readout pp1 :pp_answer_buf)
  ))
  :done)))



;  0  0  0  0  0  ([-0.9 170] [-0.8 170] [-0.4 166] [-0.3 38] [-0.2 40] [-0.1 39] [0.0 61] [0.1 49] [0.2 42] [0.3 51] [0.4 174] [0.8 163] [0.9 157]) nil991


(time (dotimes [n 10]
  (pp_train_and_answer pp1 [-1.0 1.0 1.0 1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 0.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 0.0 -1.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 1.0 0.0 0.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
  (pp_train_and_answer pp1 [-1.0 -1.0 0.0 -1.0 -1.0] [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0 0.0] {:gama (float 0.4) :eta (float 0.0005)  })
))  ;;500ms
(time (dotimes [n 10]
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 1.0 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 0.0 0.0 0.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 0.0 -1.0 0.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 1.0 0.0 0.0 -1.0])
(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0 -1.0 0.0 -1.0 -1.0])

(pp_answer pp1 [-1.0 1.0 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.9 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.8 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.7 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.6 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.5 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.45 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.4 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.3 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.2 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.1 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 0.0 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.1 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.2 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.3 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.4 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.5 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.6 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.7 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.8 1.0 1.0 -1.0])
(pp_answer pp1 [-1.0 -0.9 1.0 1.0 -1.0])
))

))

;; test_manual_pp_test_pp1  , if this false, we did not manage to test the mapping 
(is (= 
    (reduce + (map (fn [x y](if (= (float x) (float y)) 0 1))
    (pp_answer pp1 [-1.0 -1.0 0.0 -1.0 -1.0] )
    [0.0 -0.90000004 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.90000004 1.0]
    ))
    0))

)

(deftest test_manual_pp_test_pp3

(def pp3 (make_pp {:input_size 20     ;;ERROR frist half of output is incorrect
                   
                   :outputs_size 30
                   :pp_size 20
                   :rho 20                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.001)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.1)    ;;  level of error that is allowed.
                   :mu (float 1.0 )}))

(def testdata3 (make_test_array 
                 20 ;:input_size
                 30 ;:outputs_size
                 3  ;:number of items to learn
                 ))
;(pp_print_absolute_error pp3 testdata3)

(train_over_test_data 10   pp3 testdata3 false)
(train_over_test_data 2200 pp3 testdata3 false)
; (time (train_over_test_data 1000 pp3 testdata3 false))
; Verbose yes: (train_over_test_data 1000 pp3 testdata3 true)
(train_over_test_data 10   pp3 testdata3 false)

(pp_abs_errorcounts_each pp3 testdata3 0.3)
(is  (= '(0 0 0) (pp_abs_errorcounts_each pp3 testdata3 0.2)))

(pp_answer pp3 ((testdata3 0) 0))  ((testdata3 0) 1)
(pp_answer pp3 ((testdata3 1) 0))  ((testdata3 1) 1)
(pp_answer pp3 ((testdata3 2) 0))  ((testdata3 2) 1)

)

(deftest test_manual_pp_test_pp3_bufferized

(def pp3 (make_pp {:input_size 20     ;;ERROR frist half of output is incorrect
                   
                   :outputs_size 30
                   :pp_size 20
                   :rho 20                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.001)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.1)    ;;  level of error that is allowed.
                   :mu (float 1.0 )}))

(def testdata3 (bufferize_test_data (make_test_array 
                 20 ;:input_size
                 30 ;:outputs_size
                 3  ;:number of items to learn
                 )))
;(pp_print_absolute_error pp3 testdata3)

(train_over_test_data 10   pp3 testdata3 false)
(train_over_test_data 3200 pp3 testdata3 false)

(let [tain_event  (pp_train pp3 ((testdata3 1) 0) ((testdata3 1) 1) )
      tain_event2 (pp_train pp3 ((testdata3 1) 0) ((testdata3 1) 1) )
      tain_event3 (pp_train pp3 ((testdata3 2) 0) ((testdata3 2) 1) )
      m 1
      verbose? false]
  ;(println "hello" (status tain_event) (status tain_event2))
      ;(pp_train pp3 ((testdata m) 0) ((testdata m) 1) {:gama (float 0.4) :eta (float 0.0001)  })
  (if verbose? (println "hello1" (status tain_event) (status tain_event2) (status tain_event3)))
  (Thread/sleep 10)
  (if verbose? (println "hello2" (status tain_event) (status tain_event2) (status tain_event3)))
  (pp_wait_for_event pp3 tain_event2)
  (pp_enqueue_marker pp3)
  (Thread/sleep 10)
  (if verbose? (println "hello3" (status tain_event) (status tain_event2) (status tain_event3)))
  (Thread/sleep 10)
  (if verbose? (println "hello4" (status tain_event) (status tain_event2) (status tain_event3)))
          )

; (time (train_over_test_data 1000 pp3 testdata3 false))
; 
; Verbose yes: (train_over_test_data 1000 pp3 testdata3 true)
(train_over_test_data 10   pp3 testdata3 false)

(pp_abs_errorcounts_each pp3 testdata3 0.3)
(is  (= '(0 0 0) (pp_abs_errorcounts_each pp3 testdata3 0.2)))
(is  (= '(0 0 0) (pp_abs_errorcounts_each pp3 testdata3 0.11)))

(pp_abs_errorcounts_each pp3 testdata3 0.11)

;(pp_answer pp3 ((testdata3 0) 0))  ((testdata3 0) 1)
;(pp_answer pp3 ((testdata3 1) 0))  ((testdata3 1) 1)
;(pp_answer pp3 ((testdata3 2) 0))  ((testdata3 2) 1)

)





(deftest test_manual_pp_test_pp2  ;; Binary only test

(def pp2 (make_pp {:input_size 5
                   :outputs_size 3
                   :pp_size 3
                   :rho 1                    ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 0.9 )}))       ;;  learning modifier around zero   ;0.9


(pp2 :pp_queue)


(pp_readout pp2 :input_data_buf)
(pp_readout pp2 :correct_answer_buf)
(pp_readout pp2 :pp_answer_buf)
(pp_readout pp2 :vecProductResult_buf)
;(count 
  ;(pp_readout pp2 :alpha_buf)
  ;)

(def testdata2 (make_test_array 
                 5 ;:input_size
                 3 ;:outputs_size
                 4  ;:number of items to learn
                 ))

(def testdata2
    [
     [[1.0 1.0 1.0 1.0 -1.0]   [1.0 1.0 1.0]]
     [[0.0 0.0 0.0 0.0 -1.0]   [1.0 -1.0 1.0]]
     [[-1.0 0.0 -1.0 0.0 -1.0] [-1.0 1.0 1.0]]
     [[0.0 1.0 0.0 0.0 -1.0]   [-1.0 -1.0 1.0]]])

(train_over_test_data 10 pp2 testdata2 false)
(train_over_test_data 1000 pp2 testdata2 false)

(pp_answer pp2 ((testdata2 0) 0))  ((testdata2 0) 1)
(pp_answer pp2 ((testdata2 1) 0))  ((testdata2 1) 1)
(pp_answer pp2 ((testdata2 2) 0))  ((testdata2 2) 1)

(pp_answer pp2 [1.0 -1.0 -1.0 -1.0 -1.0])


)


(deftest test_manual_pp_test_pp_own_queue  
  (def pp_on_my_queue
        (make_pp {:input_size 5
                   :outputs_size 3
                   :pp_size 3
                   :rho 1                    ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 0.9 )
                   :pp_queue :my_queue }
                  )
        )
  
  (pp_on_my_queue :pp_queue)
  (pp_on_my_queue :alpha_buf)
  (@(pp_on_my_queue :pp_opencl_env) :queue)
  
  (def testdata2a (make_test_array 
                 5 ;:input_size
                 3 ;:outputs_size
                 4  ;:number of items to learn
                 ))
  
  (opencl_env_addQueue opencl_env :my_queue)
  (@opencl_env :my_queue)
  (train_over_test_data 10 
                        (conj pp_on_my_queue [:pp_queue :queue])  ;;valid openCLenv queues only.
                        testdata2a false)
  
 
  )




  
(deftest test_queue_markings
 (is (= 
        (let [pp_markings (make_pp {:input_size 5
                   :outputs_size 3
                   :pp_size 3
                   :rho 1                    ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 0.9 )})
         marker1 (pp_enqueue_marker pp_markings)
         marker2 (pp_enqueue_marker pp_markings)]

      (pp_wait_for_event pp_markings marker1)
      (status marker1)
;      (wait-for marker1)
      ;(lg_enqueue-wait-for marker1)
      (status marker1)
     (status marker2)
      ) :enqueued)
 )
  )



(quote
 (time  (test_manual_pp_test_pp1))
 (time  (test_manual_pp_test_pp2))
 (time  (test_manual_pp_test_pp3))
  ;;Typical returns
  (lg_finish ((pp1 :pp_queue) @(:pp_opencl_env pp1)))    ;; -> nil

  (def enqued_event (pp_updateAlphas pp1))                                      ;; enqued_event -> #<CLEvent Event {commandType: NDRangeKernel}>
  (status enqued_event)                                                         ;;:enqueued
  (wait-for enqued_event)                                                       ;;nil
  (status enqued_event)                                                         ;;:complete
  
  (def enqued_event (pp_updateAlphas pp1))                                      ;;:enqueued
;;  (lg_enqueue-wait-for (:queue @opencl_env) enqued_event )                      ;nil  ;;exposes its pants, especially if event is supposed to be in a pp
  (lg_enqueue-wait-for (@(pp1 :pp_opencl_env) (pp1 :pp_queue)) enqued_event )   ;nil
  (status enqued_event)                                                         ;;:enqueued
  (pp_readout pp1 :input_data_buf)                                              ;[-1.0 -1.0 0.0 -1.0 -1.0]
  (status enqued_event)                                                         ;;:complete
  
  ;;Both reads and writes complete openCL work.
  (def enqued_event (pp_updateAlphas pp1))
  (status enqued_event)
  (pp_write_input pp1 [-1.0 1.0 1.0 1.0 1.0])
  (status enqued_event)
  (pp_readout pp1 :input_data_buf)
  
  ;;What if write happened on a differet queue?
  (opencl_env_addQueue opencl_env :my_queue)
  (def enqued_event (pp_updateAlphas pp1))
  (status enqued_event)
  (pp_write_input (conj pp1 [:pp_queue :my_queue]) [-1.0 1.0 1.0 1.0 1.0])
  (status enqued_event)                                                         ;;Writse completes all queues in the environment??
  (def enqued_event (pp_updateAlphas pp1))
  (status enqued_event)
  (pp_readout (conj pp1 [:pp_queue :my_queue]) :input_data_buf)                 ;;Readout on a differnt queue does not force an event to happen
  (status enqued_event)         

  ;different event touching different buffers, same result
  (opencl_env_addQueue opencl_env :my_queue)
  (def enqued_event (pp_reduceToPP pp1))
  (status enqued_event)
  (pp_write_input (conj pp1 [:pp_queue :my_queue]) [-1.0 1.0 1.0 1.0 1.0])
  (status enqued_event)                                                         ;;Writse completes all queues in the environment??
  (def enqued_event (pp_reduceToPP pp1))
  (status enqued_event)
  (pp_readout (conj pp1 [:pp_queue :my_queue]) :input_data_buf)                 ;;Readout on a differnt queue does not force an event to happen
  (status enqued_event)

  ;;Showing that lg_enqueue-wait-for will force execution of an event on another queue
  (opencl_env_addQueue opencl_env :my_queue)
  (def enqued_event (pp_reduceToPP pp1))
  (status enqued_event)
  (pp_write_input (conj pp1 [:pp_queue :my_queue]) [-1.0 1.0 1.0 1.0 1.0])
  (status enqued_event)                                                         ;;Writse completes all queues in the environment??
  (def enqued_event (pp_reduceToPP pp1))
  (status enqued_event)
  (pp_readout (conj pp1 [:pp_queue :my_queue]) :input_data_buf)                 ;;Readout on a differnt queue does not force an event to happen
  (status enqued_event)
  (lg_enqueue-wait-for (@(pp1 :pp_opencl_env) ((conj pp1 [:pp_queue :my_queue]) :pp_queue)) enqued_event )   ;;enques a wait-for on the :my-queue, forcing event to happen on :queue before readout
  (status enqued_event)
  (pp_readout (conj pp1 [:pp_queue :my_queue]) :input_data_buf)
  (status enqued_event)
  
  ;Markers
  (def myMarker_event (lg_enqueue-marker (@(pp1 :pp_opencl_env)(pp1 :pp_queue))))
  (status myMarker_event)
  
  (= calx.data.Buffer (class (:input_data_buf pp1)))
  )




  
  
  
  

