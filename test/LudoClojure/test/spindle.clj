(ns LudoClojure.test.spindle
  (:use [LudoClojure.spindle])
  (:use [clojure.test])
  )

(use 'calx)

(comment
This is the old values that was here....
(deftest replace-me ;; FIXME: write
  (is false "No tests have been written."))
)

;Make an empty spindle
(def test_spindle (make_spindle 100 1))

(deftest test_empty_spindle
   (is (=  (test_spindle :jobid) 0))
   (is (=  (test_spindle :response) {}))
   (is (=  (peek (test_spindle :queue)) nil))
   )

(deftest test_weave_on!
   (is (= (weave_on! test_spindle #(+ 4 42)) 0))
   (is (= (weave_on! test_spindle #(+ 6 42)) 1))
   )

(deftest test_spindle_contents_after_weave_on
   (is (= (test_spindle :jobid) 2))
   (is (= @((test_spindle :response) 0) :awaiting_response))
   (is (= @((test_spindle :response) 1) :awaiting_response))
   (is (= (first (peek (test_spindle :queue))) 0))
   (is (= ((second (peek (test_spindle :queue)))) (+ 4 42)))
   )

(deftest test_spin_once!
   (is (= (spin_once! test_spindle) [0 46 true]))
   (is (= (spin_once! test_spindle) [1 48 true]))
   )

(deftest test_spool_on!
   (is (= (spool_on! test_spindle [0 "some done work" true]) "some done work"))
   (is (= @((@test_spindle :response) 0) "some done work"))
   (is (= (spool_on! test_spindle [999 "some done work2" false]) nil))
   
   (is (= ((@test_spindle :response) 999) nil))   ;nothing should be added.
   )

(deftest test_spin!
   (weave_on! test_spindle #(+ 43))
   (is (= (spin! test_spindle) 43))
   
   (weave_on! test_spindle #(+ 44))
   (weave_on! test_spindle #(+ 45))
   (is (= (spin! test_spindle) 44))
   (is (= (spin! test_spindle) 45))
   (is (= (spin! test_spindle) :nothing_to_spin))
   )


(deftest test_spin_dump! 
   (weave_on! test_spindle #(+ 46))
   (is (not (= (peek (@test_spindle :queue)) nil)))
   (spin_dump! test_spindle)
   (is (= (peek (@test_spindle :queue)) nil)))


(deftest test_spool_off!
   (let [jobid (weave_on! test_spindle #(+ 46))]
    (spin! test_spindle)
    (is (= (spool_off! test_spindle jobid)) 46))
   
   (let [jobid2 (weave_on! test_spindle #(+ 47))]
    ;;(spin! test_spindle)   result will not be available yet...
    (is (= (spool_off! test_spindle jobid2) :awaiting_response))
    (spin! test_spindle)
    (is (= (spool_off! test_spindle jobid2) 47))
    (is (= (spool_off! test_spindle jobid2) :response_missing)))
   )

(deftest test_spool_off_dump!
   (let [jobid (weave_on! test_spindle #(+ 50))]
    (spin! test_spindle)
    (is (= (spool_off! test_spindle jobid)) 50))
   (let [jobid (weave_on! test_spindle #(+ 51))]
    (spin! test_spindle)
    (spool_off_dump! test_spindle)
    (is (= (spool_off! test_spindle jobid)) nil)))
      
  
  

(deftest test_weave_off!
   (let [jobid3 (weave_on! test_spindle #(+ 48))]
     (is (= (weave_off! test_spindle jobid3) :response_timeout))
     (spin! test_spindle)
     (is (= (weave_off! test_spindle jobid3) 48))
     (is (= (weave_off! test_spindle jobid3) :response_missing))))


(deftest test_weave!
    (is (= (weave! test_spindle (fn [] (+ 4 6))) :response_timeout))
    (is (= (start_spindle! test_spindle) nil))
    (is (= (weave! test_spindle (fn [] (+ 4 7))) 11))
    (is (= (stop_spindle! test_spindle) :spindle_stopping))
    )

(deftest test_weave_closures!
   (is (= (start_spindle! test_spindle) nil))
   (is (= 
          (let [a 12 b 45]
          (weave! test_spindle (fn [] (+ a b)))) 57))
   (is (= (stop_spindle! test_spindle) :spindle_stopping))
   )




(deftest test_weave_openCL!
;Creating new spindle so that we can have it be openCL friendly from the start.
  (def test_spindle_CL (make_spindle 1000 1))  
    (def pp_openCL
  "
__kernel void foopp(
    __global float *liquidState1_a,
    __global float *liquidState1_b
    )
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
    liquidState1_b[gid] = liquidState1_a[gid] + 1.01;
}
  ")
    
   (is (= (spindle_add_openCLsource! test_spindle_CL pp_openCL) :added_openCL))
   (is (= (start_spindle! test_spindle_CL) nil))
   ;(println "about to weave on defs")
   ;(println test_spindle_CL)
   ;;If there is a timeout, this barfs violently, find out why...
   
   (weave_away! test_spindle_CL (fn [] (def openclarray  (wrap [(float 12.0) (float 12.0) (float 12.0) (float 12.0)] :float32))))
   (weave_away! test_spindle_CL (fn [] (def openclarray2 (wrap [(float 15.0) (float 16.0) (float 170.0) (float 18.0)] :float32))))
   ;(println "done opnecl defs")
   ;(println test_spindle_CL)
   
   (defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
    (enqueue-barrier)(finish)
    (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     clj_arrayout)))
     ;buffer_data)))
  ; (println "done redoutdef")

   (is (= (weave! test_spindle_CL (fn [] (readout_float_buffer openclarray2 0 4)))     (list (float 15.0) (float 16.0) (float 170.0) (float 18.0))   ))
   (weave_away! test_spindle_CL (fn [] (enqueue-kernel :foopp 4 openclarray openclarray2  )))
   (is (= (weave! test_spindle_CL (fn [] (readout_float_buffer openclarray2 0 4)))     (list (float 13.01) (float 13.01) (float 13.01) (float 13.01))))
   (is (= (nth (weave! test_spindle_CL (fn [] (readout_float_buffer openclarray2 0 4))) 0) (float 13.01)))
   (is (= (stop_spindle! test_spindle_CL) :spindle_stopping))
   )



;;This forces test to be done in sequence, which is needed here because of the 
;;statefull nature.

(deftest test_spindle_steps
   (test_empty_spindle)
   (test_weave_on!)
   (test_spindle_contents_after_weave_on)
   (test_spin_once!)
   (test_spool_on!)
   (test_spin!)
   (test_spin_dump!)
   (test_spool_off!)
   (test_spool_off_dump!)
   (test_weave_off!)
   (test_weave!)
   (test_weave_closures!)
   (test_weave_openCL!))


(defn test-ns-hook []
  (test_spindle_steps))


