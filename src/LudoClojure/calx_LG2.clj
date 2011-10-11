



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops17
;BIG onload onto GPU with clojure top level references
;Introduced coerced, mutable datatype for large vectors, saves a lot of space
;;New best practice: provide 'default' values for veraibles with (def ...) at top level, rebind them with let within function definitons...
;;This way sub statements can be run at top level + debuged without having to recreate inner function state?? aaaHHaaaaaaaa, why doesn't the literature just say that? It's so much easier to develop with closures!


(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size 4)
(def globalsize 256)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)
;(def inputvec_float2 (vec (for [i (range (* 64 64 64 32))] (float (rand)))))
;testing if we can coerce the vector data type, use (float-array..) instead of vec.
(swap! OpenCLoutputAtom2 (fn [foo] 
                            (float-array (* 64 64 64 32)
                               (for [i (range (* 64 64 64 32))] (float (rand))))))

;(def inputvec_float2 (vec (for [i (range global_clj_size)] (float (rand)))))

;(def inputvec_float3 (vec (for [i (range (* 64 64 64 32))] (rand))))

(quote
;"why is a float vector taking up so much RAM?
(def inputvec_float3 
   (vec 
      (for [i (range (* 64))] (float (rand)))
	)
)




)


(quote
;"tring out beffer creation
(def clbuffer_a (with-cl (wrap inputvec_float :float32)))
(with-cl (create-buffer :float32 32))
(create-buffer-  :float32 32 :in-out)
(with-cl (with-program (compile-program sourceOpenCL2) @(enqueue-read clbuffer_a)))
(with-cl (with-program (compile-program sourceOpenCL2)
(def clbuffer_a (wrap inputvec_float :float32))))
(with-cl (with-program (compile-program sourceOpenCL2)
(enqueue-read clbuffer_a)))
(with-cl
(def clbuffer_b (mimic clbuffer_a)))
)


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)
     ; inputvec_float (vec (for [i (range global_clj_size)] (float (rand))))
	  ]

;;(with-cl (def openCLbuffer (wrap (vec (for [i (range 10000000)] (rand))) :int32)))
;;(with-cl @(enqueue-read openCLbuffer ))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [clbuffer_a (wrap @OpenCLoutputAtom2 :float32)
          clbuffer_b (mimic clbuffer_a)
          ;c (mimic clbuffer_a)
          ;d (mimic clbuffer_a)
		  ;e (mimic clbuffer_a)
		  ;f (mimic clbuffer_a)
		  ;g (mimic clbuffer_a)
		  ;h (mimic clbuffer_a)
          cl_localsize localsize]
		  
		;(acquire! clbuffer_a)
        ;(def clbuffer_a (wrap inputvec_float2 :float32))
		;(def clbuffer_b (mimic clbuffer_a))
		;(def clbuffer_c (mimic clbuffer_a))
		;(def clbuffer_d (mimic clbuffer_a))
		;(def clbuffer_e (mimic clbuffer_a))
		
	    (def startnanotime (. System (nanoTime)))
		    (loop [k InnerLoopCount]
		          (do (enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize)
				      ;(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_d cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_e cl_localsize)
                      (enqueue-barrier)
			          (finish))
               (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))			 
      ;(swap! OpenCLoutputAtom1 (fn [foo] (deref (enqueue-read clbuffer_a))))
	  (swap! OpenCLoutputAtom1 (fn [foo] (deref (enqueue-read clbuffer_a))))
      (swap! OpenCLoutputAtom2 (fn [foo] (deref (enqueue-read clbuffer_b))))
	  ;(swap! OpenCLoutputAtom3 (fn [foo] (deref (enqueue-read clbuffer_c))))
      (release! clbuffer_a)
      (release! clbuffer_a)
	  ;(release! clbuffer_a)
    nil)))

(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))


									
									
(if	(= timingonly :timeingonly)
      (do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
        )
	   (do
	   
	      (if (> 8 (count inputvec_float))
             (println " " @OpenCLoutputAtom1 "\n " @OpenCLoutputAtom2)
	         (println "too big to print,"

	                                "\n CLin is        :" (@OpenCLoutputAtom1 (- global_clj_size 1)) 
	                                "\n CLin target    :" (* (@OpenCLoutputAtom1 (- global_clj_size 1)) (@OpenCLoutputAtom1 (- global_clj_size 1)))
									"\n CLout          :" (@OpenCLoutputAtom2 (- global_clj_size 1))
									"\n orgin Input    :" (inputvec_float (- global_clj_size 1))
									"\n orgin Output   :" (* (inputvec_float (- global_clj_size 1)) (inputvec_float (- global_clj_size 1)))
									"\n Problemsize    :" (count @OpenCLoutputAtom1)
									))
	   
         (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
								
         (def startnanotime_clj (. System (nanoTime)))
         (def testOpenCl_vs_clj(reduce (fn [coll x]
           (and coll (== (@OpenCLoutputAtom2 x) (* (@OpenCLoutputAtom1 x) (@OpenCLoutputAtom1 x)) )))
              [] (range 0 global_clj_size)))
         (def endnanotime_clj (. System (nanoTime)))
         (println "testing output accuracy: " testOpenCl_vs_clj )

         (println "Total Clojure time in ms:" (/ (- endnanotime_clj startnanotime_clj ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* (count @OpenCLoutputAtom1)) (/ (- endnanotime_clj startnanotime_clj ) 1000000000.0))) 1000000000.0) " Bln")
         ))

		 
))


(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)

(testoutputs (* 2 2 2) 2  1000 :timeingonly)

;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);




;(dorun 10 (repeatedly (fn [] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 50 b a))  ))
;(dorun 5 (repeatedly #(println "hi")))
;This is bad, takes up stack space!!
;(dorun (map (fn [] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 4 b a) (range 1 10))))
;(dorun (map #(println "hi" %) ["mum" "dad" "sister"])).

;TODO see how releaseing b will behave
;Add Calx to LudoClojure as a dependency .... more above exploratory code into LudoClojure
;;Getting problems with RAM utilisation

;1M  1600ms (when contended), but this effectively involves copying from normal ram to remote...
;10M 550ms
;;Started to run out of vRAM need to release the buffer with (release! d)?

;;http://www.overclock.net/nvidia-drivers-overclocking-software/256164-rivatuner-vram-usage.html
;;Monitoring GPU durig test runs... (especialy vRAM usage)


;(defn main [& args]
;  (println "whats with this error huh?"  @answerIs))
  
;;;





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops17
;TODO BIG onload onto GPU with clojure top level references
;DONE Introduced coerced, mutable datatype for large vector
;DONE loat the large dataset into the atom,  This is sustainable for ongoig loops
;SAVE and LOAD from file, 
;;New best practice: provide 'default' values for veraibles with (def ...) at top level, rebind them with let within function definitons...
;;This way sub statements can be run at top level + debuged without having to recreate inner function state?? aaaHHaaaaaaaa, why doesn't the literature just say that? It's so much easier to develop with closures!


(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size 4)
(def globalsize 256)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)
;(def inputvec_float2 (vec (for [i (range (* 64 64 64 32))] (float (rand)))))
;testing if we can coerce the vector data type, use (float-array..) instead of vec.
(swap! OpenCLoutputAtom2 (fn [foo] 
                               (^floats float-array (* 64 64 64 32)
                                   (for [i (range (* 64 64 64 32))] (float (rand))))))  ;Optimal sequece based float-arrat atom creation.
(println "(type @OpenCLoutputAtom2) :" (type @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))

								   ;(def ^floats inputvec_float2 (float-array (* 64 64 64 32)
;     (for [i (range (* 64 64 64 32))] (float (rand)))))

;(def inputvec_float2 (vec (for [i (range global_clj_size)] (float (rand)))))

;(def inputvec_float3 (vec (for [i (range (* 64 64 64 32))] (rand))))

(quote
(time (swap! OpenCLoutputAtom4 (fn [foo] 
                               (^floats float-array (* 64 64 64 64)
                                   (for [i (range (* 64 64 64 64))] (float (rand)))))))
(count @OpenCLoutputAtom3)

)


(quote
;"tring out beffer creation
(def clbuffer_a (with-cl (wrap inputvec_float :float32)))
(with-cl (create-buffer :float32 32))
(create-buffer-  :float32 32 :in-out)
(with-cl (with-program (compile-program sourceOpenCL2) @(enqueue-read clbuffer_a)))
(with-cl (with-program (compile-program sourceOpenCL2)
(def clbuffer_a (wrap inputvec_float :float32))))
(with-cl (with-program (compile-program sourceOpenCL2)
(enqueue-read clbuffer_a)))
(with-cl
(def clbuffer_b (mimic clbuffer_a)))
)


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)
     ; inputvec_float (vec (for [i (range global_clj_size)] (float (rand))))
	  ]

;;(with-cl (def openCLbuffer (wrap (vec (for [i (range 10000000)] (rand))) :int32)))
;;(with-cl @(enqueue-read openCLbuffer ))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [;clbuffer_a (wrap @OpenCLoutputAtom2 :float32)
          ;clbuffer_b (mimic clbuffer_a)
          ;c (mimic clbuffer_a)
          ;d (mimic clbuffer_a)
		  ;e (mimic clbuffer_a)
		  ;f (mimic clbuffer_a)
		  ;g (mimic clbuffer_a)
		  ;h (mimic clbuffer_a)
          cl_localsize localsize]
		  
		;(acquire! clbuffer_a)
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(def clbuffer_b (mimic clbuffer_a))
		;(def clbuffer_c (mimic clbuffer_a))
		;(def clbuffer_d (mimic clbuffer_a))
		;(def clbuffer_e (mimic clbuffer_a))
		
	    (def startnanotime (. System (nanoTime)))
		    (loop [k InnerLoopCount]
		          (do (enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize)
				      ;(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_d cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_e cl_localsize)
                      (enqueue-barrier)
			          (finish))
               (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))			 
      ;(swap! OpenCLoutputAtom1 (fn [foo] (deref (enqueue-read clbuffer_a))))  ;we don't cate about old a, which was the atom value beforehand...
      (def startnanotime_bufferReadOutTime (. System (nanoTime)))
	  (swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT
	  (def endnanotime_bufferReadOutTime (. System (nanoTime)))
	  ;(swap! OpenCLoutputAtom3 (fn [foo] (deref (enqueue-read clbuffer_c))))
      (release! clbuffer_a)
      (release! clbuffer_b)
	  ;(release! clbuffer_a)
    nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))

									
									
(if	(= timingonly :timeingonly)
      (do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)
	   
	   (do
	   
	      (if (> 8 (count inputvec_float))
             (println " " @OpenCLoutputAtom1 "\n " @OpenCLoutputAtom2)
	         (println "too big to print,"

	                                "\n CLin is        :" (@OpenCLoutputAtom1 (- global_clj_size 1)) 
	                                "\n CLin target    :" (* (@OpenCLoutputAtom1 (- global_clj_size 1)) (@OpenCLoutputAtom1 (- global_clj_size 1)))
									"\n CLout          :" (@OpenCLoutputAtom2 (- global_clj_size 1))
									"\n orgin Input    :" (inputvec_float (- global_clj_size 1))
									"\n orgin Output   :" (* (inputvec_float (- global_clj_size 1)) (inputvec_float (- global_clj_size 1)))
									"\n Problemsize    :" (count @OpenCLoutputAtom1)
									))
	   
         (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
								
         (def startnanotime_clj (. System (nanoTime)))
        ; (def testOpenCl_vs_clj (reduce (fn [coll x]   ;Note can not do full test because original value is lost, may still keep copy of old during development to allow for testing for correct output out of openCL kernel
        ;   (conj coll (== (nth @OpenCLoutputAtom2 x) (* (nth @OpenCLoutputAtom1 x) (nth @OpenCLoutputAtom1 x)) )))
        ;      [] (range 0 global_clj_size)))
		(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))	  
			  
         (def endnanotime_clj (. System (nanoTime)))
         ;(println "testing output accuracy: " testOpenCl_vs_clj )

         (println "Total Clojure time in ms:" (/ (- endnanotime_clj startnanotime_clj ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* (count @OpenCLoutputAtom1)) (/ (- endnanotime_clj startnanotime_clj ) 1000000000.0))) 1000000000.0) " Bln")
         ))

		 
))


(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)

;(testoutputs (* 2 2 2) 2  1000 :timeingonly)
;(testoutputs (* 64 64 64 32) 1  3000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops18
;SAVE and LOAD from file, 
;TODO BIG onload onto GPU with clojure top level references
;DONE Introduced coerced, mutable datatype for large vector
;DONE loat the large dataset into the atom,  This is sustainable for ongoig loops
;;New best practice: provide 'default' values for veraibles with (def ...) at top level, rebind them with let within function definitons...
;;This way sub statements can be run at top level + debuged without having to recreate inner function state?? aaaHHaaaaaaaa, why doesn't the literature just say that? It's so much easier to develop with closures!


(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size 4)
(def globalsize 256)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)
;(def inputvec_float2 (vec (for [i (range (* 64 64 64 32))] (float (rand)))))
;testing if we can coerce the vector data type, use (float-array..) instead of vec.
(swap! OpenCLoutputAtom2 (fn [foo] 
                               (^floats float-array (* 64 64 64 32)
                                   (for [i (range (* 64 64 64 32))] (float (rand))))))  ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
(println "(type @OpenCLoutputAtom2) :" (type @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))

								   ;(def ^floats inputvec_float2 (float-array (* 64 64 64 32)
;     (for [i (range (* 64 64 64 32))] (float (rand)))))

;(def inputvec_float2 (vec (for [i (range global_clj_size)] (float (rand)))))

;(def inputvec_float3 (vec (for [i (range (* 64 64 64 32))] (rand))))

(quote
(time (swap! OpenCLoutputAtom4 (fn [foo] 
                               (^floats float-array (* 64 64 64 64 2)
                                   (for [i (range (* 64 64 64 64 2))] (float (rand)))))))
(time (swap! OpenCLoutputAtom4 (fn [foo] 
                               (^floats float-array (* 64 64 64 32)
                                   (for [i (range (* 64 64 64 32))] (float (rand)))))))
(* 64 64 64)
								   
(count @OpenCLoutputAtom3)

)


(quote
;"tring out beffer creation
(def clbuffer_a (with-cl (wrap inputvec_float :float32)))
(with-cl (create-buffer :float32 32))
(create-buffer-  :float32 32 :in-out)
(with-cl (with-program (compile-program sourceOpenCL2) @(enqueue-read clbuffer_a)))
(with-cl (with-program (compile-program sourceOpenCL2)
(def clbuffer_a (wrap inputvec_float :float32))))
(with-cl (with-program (compile-program sourceOpenCL2)
(enqueue-read clbuffer_a)))
(with-cl
(def clbuffer_b (mimic clbuffer_a)))
)


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)
     ; inputvec_float (vec (for [i (range global_clj_size)] (float (rand))))
	  ]

;;(with-cl (def openCLbuffer (wrap (vec (for [i (range 10000000)] (rand))) :int32)))
;;(with-cl @(enqueue-read openCLbuffer ))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [;clbuffer_a (wrap @OpenCLoutputAtom2 :float32)
          ;clbuffer_b (mimic clbuffer_a)
          ;c (mimic clbuffer_a)
          ;d (mimic clbuffer_a)
		  ;e (mimic clbuffer_a)
		  ;f (mimic clbuffer_a)
		  ;g (mimic clbuffer_a)
		  ;h (mimic clbuffer_a)
          cl_localsize localsize]
		  
		;(acquire! clbuffer_a)
		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
		(def clbuffer_b (mimic clbuffer_a))
		;(def clbuffer_c (mimic clbuffer_a))
		;(def clbuffer_d (mimic clbuffer_a))
		;(def clbuffer_e (mimic clbuffer_a))
		
	    (def startnanotime (. System (nanoTime)))
		    (loop [k InnerLoopCount]
		          (do (enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize)
				      ;(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_d cl_localsize)
					  ;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_e cl_localsize)
                      (enqueue-barrier)
			          (finish))
               (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))			 
      ;(swap! OpenCLoutputAtom1 (fn [foo] (deref (enqueue-read clbuffer_a))))  ;we don't cate about old a, which was the atom value beforehand...
      (def startnanotime_bufferReadOutTime (. System (nanoTime)))
	  (swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;Nuke the old data so that the write it will not take double space due to the transactional swap!
	  (swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
	  (def endnanotime_bufferReadOutTime (. System (nanoTime)))
	  ;(swap! OpenCLoutputAtom3 (fn [foo] (deref (enqueue-read clbuffer_c))))
      (release! clbuffer_a)
      (release! clbuffer_b)
	  ;(release! clbuffer_a)
    nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))

									
									
(if	(= timingonly :timeingonly)
      (do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)
	   
	   (do
	   
	      (if (> 8 (count inputvec_float))
             (println " " @OpenCLoutputAtom1 "\n " @OpenCLoutputAtom2)
	         (println "too big to print,"

	                                "\n CLin is        :" (@OpenCLoutputAtom1 (- global_clj_size 1)) 
	                                "\n CLin target    :" (* (@OpenCLoutputAtom1 (- global_clj_size 1)) (@OpenCLoutputAtom1 (- global_clj_size 1)))
									"\n CLout          :" (@OpenCLoutputAtom2 (- global_clj_size 1))
									"\n orgin Input    :" (inputvec_float (- global_clj_size 1))
									"\n orgin Output   :" (* (inputvec_float (- global_clj_size 1)) (inputvec_float (- global_clj_size 1)))
									"\n Problemsize    :" (count @OpenCLoutputAtom1)
									))
	   
         (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
         "\noperations per second" (/ (bigint(/ (* InnerLoopCount (count @OpenCLoutputAtom1)) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln")
								
         (def startnanotime_clj (. System (nanoTime)))
        ; (def testOpenCl_vs_clj (reduce (fn [coll x]   ;Note can not do full test because original value is lost, may still keep copy of old during development to allow for testing for correct output out of openCL kernel
        ;   (conj coll (== (nth @OpenCLoutputAtom2 x) (* (nth @OpenCLoutputAtom1 x) (nth @OpenCLoutputAtom1 x)) )))
        ;      [] (range 0 global_clj_size)))
		(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 1))	  
			  
         (def endnanotime_clj (. System (nanoTime)))
         ;(println "testing output accuracy: " testOpenCl_vs_clj )

         (println "Total Clojure time in ms:" (/ (- endnanotime_clj startnanotime_clj ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* (count @OpenCLoutputAtom1)) (/ (- endnanotime_clj startnanotime_clj ) 1000000000.0))) 1000000000.0) " Bln")
         ))

		 
))


(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)

;(testoutputs (* 2 2 2) 2  1000 :timeingonly)
;(testoutputs (* 64 64 64 32) 1  3000 :timeingonly)
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)

;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops19
;CleanUp, 
;Load in a buffer incrementally...
;Read out a buffer incrementally...
;"There is no spoon"  ...   (wrap...) creates the openCL buffer, it can be consumed by a enqueue-kernel directly... Without RAM Cost??
(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size (* 64 64 64))
(def globalsize global_clj_size)
(def localsize 16)
(def InnerLoopCount 16)
(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)

(defn swapIn_OpenCLoutputAtom2! [global_clj_size]
  (swap! OpenCLoutputAtom2 (fn [foo] [0]))   ; A pre data nuke to reduce RAM footptint during swapin
  (swap! OpenCLoutputAtom2 (fn [foo] 
                               (^floats float-array global_clj_size
                                   (for [i (range global_clj_size)] (float (rand)))))) ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
  (println "(type @OpenCLoutputAtom2) :" (type @OpenCLoutputAtom2))
  (println "(nth @OpenCLoutputAtom2 0):" (nth @OpenCLoutputAtom2 0)))
								   


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)]

;;(def CustomLargeByteBuffer  (create-byte-buffer [(* 64 64)]))
;;(println "type CustomLargeByteBuffer: "(type CustomLargeByteBuffer))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
	(let [cl_localsize localsize
	      CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 )    ]

		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN
		  ;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;Nuke the atom's contents as data is now on the GPU
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
        (def clbuffer_b (mimic clbuffer_a))
		(def clbuffer_c (mimic clbuffer_a))
		
		(println "type clbuffer_a: "(type clbuffer_a))
		(println "type CustomLargeBuffer: "(type CustomLargeBuffer))
		;(def CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))     ;Createing arbitrary large buffer
		
		;;Would be nice to be ableto to write to it...
		
	;	(enqueue-overwrite clbuffer_c [0 3] 
	;	  (.createByteBuffer (context) CLMem$Usage/InputOutput (to-buffer [0.13 0.23 0.34 0.45] :float32) false))  ;(usage-types :in-out)-> CLMem$Usage/InputOutput
		;OK  (def  CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))
		;BROKEN(CustomLargeBuffer (to-buffer (wrap @OpenCLoutputAtom2 :float32) :float32))

		;;There seem to be two types of buffer... calx.data.Buffer and java.nio.ByteBuffer.
		
		(enqueue-barrier) (finish)
	    (def startnanotime (. System (nanoTime)))
		  (loop [k InnerLoopCount]
		          (do 	(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b              cl_localsize) (enqueue-barrier)  
						(enqueue-kernel :looper globalsize (wrap @OpenCLoutputAtom2 :float32) CustomLargeBuffer cl_localsize) (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b              cl_localsize) (enqueue-barrier)
						(finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))
		
		(def startnanotime_bufferReadOutTime (. System (nanoTime)))
		(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;NOTE; already is nuked by this point since the load.Nuke the old data so that the write it will not take double space due to the transactional swap!
		(swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
		(def endnanotime_bufferReadOutTime (. System (nanoTime)))
		
		(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read CustomLargeBuffer [0 24])))))   ;OPTIMAL RADOUT OPTION: Could loop to read out parts of the buffer at a time OPTIMAL 
		(enqueue-barrier) (finish)
		(release! clbuffer_a)
		(release! clbuffer_b)
		(release! clbuffer_c)
		(release! CustomLargeBuffer)   ;Note, without enqueue-read, we are leaking GPU RAM.    JAVA GC (garbace collector) needs to kick in to actualy release the reserved data from the GPU, distroying old versions that got (def...) earlier. This takes time(can not be forced?) and thuse it will be possible to run out of RAM.  Recomendation is to stay within the inner kernel loop, swapping data into buffers, reading out when needed... but never 'top level' looping, thus effectively working with constant GPU RAM utalisation via buffer overwrites. Just need to figure direct 'write in' into an existing buffer.
		(enqueue-barrier)
		(finish)
	nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 0))

(println (vec @OpenCLoutputAtom3))
									
									
(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)

		 
))



(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)


(def global_clj_size (* 16))
(swapIn_OpenCLoutputAtom2! (* global_clj_size 1))
(testoutputs global_clj_size 1  10 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)


;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops20
;Max GPU onload.
;Load in a buffer incrementally...
;Read out a buffer incrementally...
;"There is no spoon"  ...   (wrap...) creates the openCL buffer, it can be consumed by a enqueue-kernel directly... Without RAM Cost??
(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size (* 64 64 64))
(def globalsize global_clj_size)
(def localsize 16)
(def InnerLoopCount 16)
;(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)

(defn swapIn_OpenCLoutputAtom2! [global_clj_size]
  (swap! OpenCLoutputAtom2 (fn [foo] [0]))   ; A pre data nuke to reduce RAM footptint during swapin
  (swap! OpenCLoutputAtom2 (fn [foo] 
                               (^floats float-array global_clj_size
                                   (for [i (range global_clj_size)] (float (rand)))))) ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
  (println "(type @OpenCLoutputAtom2) :" (type @OpenCLoutputAtom2))
  (println "(nth @OpenCLoutputAtom2 0):" (nth @OpenCLoutputAtom2 0)))
								   


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)]

;;(def CustomLargeByteBuffer  (create-byte-buffer [(* 64 64)]))
;;(println "type CustomLargeByteBuffer: "(type CustomLargeByteBuffer))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
	(let [cl_localsize localsize
	     ; CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 )    ;create an empty buffer
		  ;clbuffer_b (create-buffer global_clj_size :float32 )
          ]
		  
		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN  --OR direct (wrap...) into kernel
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;Nuke the atom's contents as data is now on the GPU
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
        (def clbuffer_b (mimic clbuffer_a))
		(def clbuffer_c (mimic clbuffer_a))
		
		;(println "type clbuffer_a: "(type clbuffer_a))
		;(println "type CustomLargeBuffer: "(type CustomLargeBuffer))
		;(def CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))     ;Createing arbitrary large buffer
		
		;;Would be nice to be ableto to write to it...
		
	;	(enqueue-overwrite clbuffer_c [0 3] 
	;	  (.createByteBuffer (context) CLMem$Usage/InputOutput (to-buffer [0.13 0.23 0.34 0.45] :float32) false))  ;(usage-types :in-out)-> CLMem$Usage/InputOutput
		;OK  (def  CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))
		;BROKEN(CustomLargeBuffer (to-buffer (wrap @OpenCLoutputAtom2 :float32) :float32))

		;;There seem to be two types of buffer... calx.data.Buffer and java.nio.ByteBuffer.
		
		(enqueue-barrier) (finish)
	    (def startnanotime (. System (nanoTime)))
		  (loop [k InnerLoopCount]
		          (do 	;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b              cl_localsize) (enqueue-barrier)  
						(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize) (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c              cl_localsize) (enqueue-barrier)
						(finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))
		
		(def startnanotime_bufferReadOutTime (. System (nanoTime)))
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;NOTE; already is nuked by this point since the load.Nuke the old data so that the write it will not take double space due to the transactional swap!
	;	(swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
		(def endnanotime_bufferReadOutTime (. System (nanoTime)))
		
		;(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read CustomLargeBuffer [0 24])))))   ;OPTIMAL RADOUT OPTION: Could loop to read out parts of the buffer at a time OPTIMAL 
		(enqueue-barrier) (finish)
		;(release! clbuffer_a)
		(release! clbuffer_b)
		;(release! clbuffer_c)
		;(release! CustomLargeBuffer)   ;Note, without enqueue-read, we are leaking GPU RAM.    JAVA GC (garbace collector) needs to kick in to actualy release the reserved data from the GPU, distroying old versions that got (def...) earlier. This takes time(can not be forced?) and thuse it will be possible to run out of RAM.  Recomendation is to stay within the inner kernel loop, swapping data into buffers, reading out when needed... but never 'top level' looping, thus effectively working with constant GPU RAM utalisation via buffer overwrites. Just need to figure direct 'write in' into an existing buffer.
		(enqueue-barrier)
		(finish)
	nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 0))

(println (vec @OpenCLoutputAtom3))
									
									
(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)

		 
))



(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)


(def global_clj_size (* 64 64 64 64 2))
(swapIn_OpenCLoutputAtom2! (* global_clj_size 1))
(testoutputs global_clj_size 1  1000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)


;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops21
;


(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))
(def OpenCLoutputAtom4 (atom [1]))
(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size (* 64 64 64))
(def globalsize global_clj_size)
(def localsize 16)
(def InnerLoopCount 16)
;(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)

(defn swapIn_OpenCLoutputAtom2! [global_clj_size]
  (swap! OpenCLoutputAtom2 (fn [foo] [0]))   ; A pre data nuke to reduce RAM footptint during swapin
  (swap! OpenCLoutputAtom2 (fn [foo] 
                               (^floats float-array global_clj_size
                                   (for [i (range global_clj_size)] (float (rand)))))) ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
  (println "(type @OpenCLoutputAtom2) :" (type @OpenCLoutputAtom2))
  (println "(nth @OpenCLoutputAtom2 0):" (nth @OpenCLoutputAtom2 0)))
								   


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)]

;;(def CustomLargeByteBuffer  (create-byte-buffer [(* 64 64)]))
;;(println "type CustomLargeByteBuffer: "(type CustomLargeByteBuffer))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
	(let [cl_localsize localsize
	     ; CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 )    ;create an empty buffer
		  ;clbuffer_b (create-buffer global_clj_size :float32 )
          ]
		  
		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN  --OR direct (wrap...) into kernel
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;Nuke the atom's contents as data is now on the GPU
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
        (def clbuffer_b (mimic clbuffer_a))
		(def clbuffer_c (mimic clbuffer_a))
		
		;(println "type clbuffer_a: "(type clbuffer_a))
		;(println "type CustomLargeBuffer: "(type CustomLargeBuffer))
		;(def CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))     ;Createing arbitrary large buffer
		
		;;Would be nice to be ableto to write to it...
		
	;	(enqueue-overwrite clbuffer_c [0 3] 
	;	  (.createByteBuffer (context) CLMem$Usage/InputOutput (to-buffer [0.13 0.23 0.34 0.45] :float32) false))  ;(usage-types :in-out)-> CLMem$Usage/InputOutput
		;OK  (def  CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))
		;BROKEN(CustomLargeBuffer (to-buffer (wrap @OpenCLoutputAtom2 :float32) :float32))

		;;There seem to be two types of buffer... calx.data.Buffer and java.nio.ByteBuffer.
		
		(enqueue-barrier) (finish)
	    (def startnanotime (. System (nanoTime)))
		  (loop [k InnerLoopCount]
		          (do 	;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b              cl_localsize) (enqueue-barrier)  
						(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize) (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c              cl_localsize) (enqueue-barrier)
						(finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))
		
		(def startnanotime_bufferReadOutTime (. System (nanoTime)))
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;NOTE; already is nuked by this point since the load.Nuke the old data so that the write it will not take double space due to the transactional swap!
	;	(swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
		(def endnanotime_bufferReadOutTime (. System (nanoTime)))
		
		;(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read CustomLargeBuffer [0 24])))))   ;OPTIMAL RADOUT OPTION: Could loop to read out parts of the buffer at a time OPTIMAL 
		(enqueue-barrier) (finish)
		;(release! clbuffer_a)
		(release! clbuffer_b)
		;(release! clbuffer_c)
		;(release! CustomLargeBuffer)   ;Note, without enqueue-read, we are leaking GPU RAM.    JAVA GC (garbace collector) needs to kick in to actualy release the reserved data from the GPU, distroying old versions that got (def...) earlier. This takes time(can not be forced?) and thuse it will be possible to run out of RAM.  Recomendation is to stay within the inner kernel loop, swapping data into buffers, reading out when needed... but never 'top level' looping, thus effectively working with constant GPU RAM utalisation via buffer overwrites. Just need to figure direct 'write in' into an existing buffer.
		(enqueue-barrier)
		(finish)
	nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 0))

(println (vec @OpenCLoutputAtom3))
									
									
(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)

		 
))



(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)


(def global_clj_size (* 64 64 2))
(swapIn_OpenCLoutputAtom2! (* global_clj_size 1))
(testoutputs global_clj_size 1  1000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)


;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops22
;Cleanup, macro and lispation


(def sourceOpenCL2
  " 
__kernel void looper(
    __global float *input,
    __global float *output,
    const unsigned int localloopsize)
{
    int iatom;
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
	
    for( iatom = 0; iatom < localloopsize; iatom+=1 )
        output[iatom + gid*localloopsize] = input[iatom + gid*localloopsize] * input[iatom + gid*localloopsize];
}
  ")
  
;(def OpenCLoutputAtom1 (atom [1]))
;(def OpenCLoutputAtom2 (atom [1]))
;(def OpenCLoutputAtom3 (atom [1]))
;(def OpenCLoutputAtom4 (atom [1]))
;(def OpenCLoutputAtom5 (atom [1]))

(def global_clj_size (* 64 64 64))
(def globalsize global_clj_size)
(def localsize 16)
(def InnerLoopCount 16)
;(def inputvec_float (vec (for [i (range global_clj_size)] (float (rand)))))
(def timingonly :timeingonly)





								   


(defn testoutputs [global local InnerLoopCount timingonly]
(let [globalsize global
      localsize local
      global_clj_size (* local global)]

;;(def CustomLargeByteBuffer  (create-byte-buffer [(* 64 64)]))
;;(println "type CustomLargeByteBuffer: "(type CustomLargeByteBuffer))
	  
(with-cl
  (with-program (compile-program sourceOpenCL2)
	(let [cl_localsize localsize
	     ; CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 )    ;create an empty buffer
		  ;clbuffer_b (create-buffer global_clj_size :float32 )
          ]
		  
		(println "about to onload buffer")		
		(def startnanotime_bufferReadInTime (. System (nanoTime)))
        (def clbuffer_a (wrap @OpenCLoutputAtom2 :float32))    ;OPTIMAL READIN  --OR direct (wrap...) into kernel
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;Nuke the atom's contents as data is now on the GPU
		(def endnanotime_bufferReadInTime (. System (nanoTime)))
		(println "onload buffer done")
        (def clbuffer_b (mimic clbuffer_a))
		(def clbuffer_c (mimic clbuffer_a))
		
		;(println "type clbuffer_a: "(type clbuffer_a))
		;(println "type CustomLargeBuffer: "(type CustomLargeBuffer))
		;(def CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))     ;Createing arbitrary large buffer
		
		;;Would be nice to be ableto to write to it...
		
	;	(enqueue-overwrite clbuffer_c [0 3] 
	;	  (.createByteBuffer (context) CLMem$Usage/InputOutput (to-buffer [0.13 0.23 0.34 0.45] :float32) false))  ;(usage-types :in-out)-> CLMem$Usage/InputOutput
		;OK  (def  CustomLargeBuffer (create-buffer (* 64 64 64 64) :float32 ))
		;BROKEN(CustomLargeBuffer (to-buffer (wrap @OpenCLoutputAtom2 :float32) :float32))

		;;There seem to be two types of buffer... calx.data.Buffer and java.nio.ByteBuffer.
		
		(enqueue-barrier) (finish)
	    (def startnanotime (. System (nanoTime)))
		  (loop [k InnerLoopCount]
		          (do 	;(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b              cl_localsize) (enqueue-barrier)  
						(enqueue-kernel :looper globalsize clbuffer_a clbuffer_b cl_localsize) (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						(enqueue-kernel :looper globalsize clbuffer_b clbuffer_c              cl_localsize) (enqueue-barrier)
						(finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime (. System (nanoTime)))
		
		(def startnanotime_bufferReadOutTime (. System (nanoTime)))
		;(swap! OpenCLoutputAtom2 (fn [foo] [0]))  ;NOTE; already is nuked by this point since the load.Nuke the old data so that the write it will not take double space due to the transactional swap!
	;	(swap! OpenCLoutputAtom2 (fn [foo] (^floats float-array (deref (enqueue-read clbuffer_b)))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
		(def endnanotime_bufferReadOutTime (. System (nanoTime)))
		
		;(swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read CustomLargeBuffer [0 24])))))   ;OPTIMAL RADOUT OPTION: Could loop to read out parts of the buffer at a time OPTIMAL 
		(enqueue-barrier) (finish)
		;(release! clbuffer_a)
		(release! clbuffer_b)
		;(release! clbuffer_c)
		;(release! CustomLargeBuffer)   ;Note, without enqueue-read, we are leaking GPU RAM.    JAVA GC (garbace collector) needs to kick in to actualy release the reserved data from the GPU, distroying old versions that got (def...) earlier. This takes time(can not be forced?) and thuse it will be possible to run out of RAM.  Recomendation is to stay within the inner kernel loop, swapping data into buffers, reading out when needed... but never 'top level' looping, thus effectively working with constant GPU RAM utalisation via buffer overwrites. Just need to figure direct 'write in' into an existing buffer.
		(enqueue-barrier)
		(finish)
	nil)))

(println "(type @OpenCLoutputAtom2):" (type @OpenCLoutputAtom2))
(count @OpenCLoutputAtom1)
(count @OpenCLoutputAtom2)
(println "(count @OpenCLoutputAtom1):" (count @OpenCLoutputAtom1))
(println "(count @OpenCLoutputAtom2):" (count @OpenCLoutputAtom2))
(println "(nth @OpenCLoutputAtom2 1):" (nth @OpenCLoutputAtom2 0))

(println (vec @OpenCLoutputAtom3))
									
									
(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime startnanotime ) 1000000.0)
           "\noperations per second" (/ (bigint(/ (* InnerLoopCount global_clj_size) (/ (- endnanotime startnanotime ) 1000000000.0))) 1000000000.0) " Bln"
		   "\nTotal OpenCL bufferread In  Time in ms:" (/ (- endnanotime_bufferReadInTime  startnanotime_bufferReadInTime  ) 1000000.0)
		   "\nTotal OpenCL bufferread Out Time in ms:" (/ (- endnanotime_bufferReadOutTime startnanotime_bufferReadOutTime ) 1000000.0))
		)

		 
))



(println "example code \n (testoutputs (expt 2 19) 1  100)  ");7.7
(quote
(testoutputs (* 64 64 64 32) 1  1000 :timeingonly)
(testoutputs (* 64 64 64) 8  1000)
(testoutputs (* 64 64 32) 16  1000)
(testoutputs (* 64 64 2) 2  10)
)


(defmacro swapIn_atom! [atomname global_clj_size atomgeneraorfunction]
  `(let [global_clj_size# ~global_clj_size]
	(def ~atomname (atom []))   ; A pre data nuke to reduce RAM footptint during swapin
    (swap! ~atomname (fn [~atomname] (~atomgeneraorfunction global_clj_size#))) ;Optimal sequece based float-arrat atom creation. ; nice beucase of almost instant alocation
    (println "(type @" '~atomname ") :" (type (deref ~atomname)))
    (println "(nth @" '~atomname " 0):" (nth (deref ~atomname) 0))))
;;Takeaway:
;; ~atomname    get replaced at macro expand
;;  #foo is gensym, unique symbol for within macro only (safe to use names that will not polute global scope)


(macroexpand-1 '(swapIn_atom! OpenCLoutputAtom2 (* global_clj_size 1) randomarraygenerator))

(defmacro makeatom! [atomame]
  `(do (def ~atomame (atom [1]))
   (swap! ~atomame (fn [~atomame] 1))))

 (do (def boo (atom [1]))
   (swap! boo (fn [_] 1)))
   
(makeatom! fooooo)
(macroexpand-1 '(makeatom! fooooo))
@fooooo
(class fooooo)

(string? 'OpenCLoutputAtom2)
(resolve 'foo)


;;Note, as we develop better liquids, we can just define them with these liquid generating functions
(defn randomarraygenerator [global_clj_size]
  (^floats float-array global_clj_size
    (for [i (range global_clj_size)] (float (rand)))))
	
(def global_clj_size (* 64 64 64 4))
;(swapIn_OpenCLoutputAtom2! (* global_cljze_si 1))
(swapIn_atom! OpenCLoutputAtom2 (* global_clj_size 1) randomarraygenerator)
(swapIn_atom! OpenCLoutputAtom3 16 randomarraygenerator)
(testoutputs global_clj_size 1  10000 :timeingonly)
;(testoutputs (* 2 2 2) 2  10 :not)


;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops24
;Random number generator    , towards on-the-fly, self materalising liquids

  
(def sourceOpenCL
  "

int randomnumber_fun(int m_z_in, int m_w_in)
{
int m_z;
int m_w;
m_z = 36969 * (m_z_in & 65535) + (m_z_in >> 16);
m_w = 18000 * (m_w_in & 65535) + (m_w_in >> 16);
return (m_z << 16) + m_w;
}
  
__kernel void randomnumbergen(
    __global int *input_i,
    __global uint *output_i,
	__global float *output_f)
{

    int gid = get_global_id(0);
    int gsize = get_global_size(0);

	int m_z = gid;
	int m_w = 1;
	int randomnumber = 1;
	m_z = 36969 * (m_z & 65535) + (m_z >> 16);
    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
	randomnumber = (m_z << 16) + m_w;

    output_i[gid] = randomnumber_fun(gid, 1);
	output_f[gid] = 1.0 / gid ;
	
	
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))



;m_w = <choose-initializer>;    /* must not be zero */
;m_z = <choose-initializer>;    /* must not be zero */
; 
;uint get_random()
;{
;    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
;    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
;    return (m_z << 16) + m_w;  /* 32-bit result */
;}
(def gloal_size_clj (* 64 64 64 64 4))
(def innerloop_clj 1000)

(with-cl
  (with-program (compile-program sourceOpenCL)
	(let [gloal_size gloal_size_clj
	      InnerLoopCount innerloop_clj]
		  
		(println "about to onload buffer")
		(def startnanotime_bufferCreateTime (. System (nanoTime)))
	    (def Buffer_int1   (create-buffer gloal_size :int32  ))
		(def Buffer_int2   (create-buffer gloal_size :int32  ))
		(def Buffer_float  (create-buffer gloal_size :float32)) 		
		(enqueue-barrier) (finish)		
		(def endnanotime_bufferCreateTime (. System (nanoTime)))
		
	    (def startnanotime_kerneltime (. System (nanoTime)))
		  (loop [k InnerLoopCount]
		          (do
						(enqueue-kernel :randomnumbergen gloal_size Buffer_int1 Buffer_int2 Buffer_float) 
						(enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						(finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
	    (def endnanotime_kerneltime (. System (nanoTime)))
		
		(def startnanotime_bufferReadOutTime (. System (nanoTime)))
		
        (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
        (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 6400])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
        (swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
 
		(def endnanotime_bufferReadOutTime (. System (nanoTime)))
        (enqueue-barrier) (finish)
		(release! Buffer_float)
		(enqueue-barrier)
		(finish)
	nil)))

(println (vec @OpenCLoutputAtom1))
;(println (vec @OpenCLoutputAtom2))
(println (vec @OpenCLoutputAtom3))

(println(count @OpenCLoutputAtom2))
(println(reduce + @OpenCLoutputAtom2) )
(println(/ (reduce + @OpenCLoutputAtom2) (count @OpenCLoutputAtom2) ))

(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000.0)
           "\nnumber operations per second:" (/ (bigint(/ (* gloal_size_clj innerloop_clj) (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000000.0))) 1000000000.0) " Bln"
		))



		
;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops25
;Random number generator    , towards on-the-fly, self materalising liquids
;First UI just to show the random numbers....
  
(def sourceOpenCL
  "

uint randomnumber_fun(uint m_z_in, uint m_w_in)
{
uint m_z;
uint m_w;
m_z = 36969 * (m_z_in & 65535) + (m_z_in >> 16);
m_w = 18000 * (m_w_in & 65535) + (m_w_in >> 16);
return (m_z << 16) + m_w;
}
  
__kernel void randomnumbergen(
    __global uint *output_i)
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
    output_i[gid] = randomnumber_fun(gid, gid ) % 1000;

}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))



;m_w = <choose-initializer>;    /* must not be zero */
;m_z = <choose-initializer>;    /* must not be zero */
; 
;uint get_random()
;{
;    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
;    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
;    return (m_z << 16) + m_w;  /* 32-bit result */
;}
(def gloal_size_clj (* 64 64))
(def innerloop_clj 10)

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [gloal_size gloal_size_clj
        InnerLoopCount innerloop_clj]
        
        (println "about to onload buffer")
        (def startnanotime_bufferCreateTime (. System (nanoTime)))
        (def Buffer_int1   (create-buffer gloal_size :int32  ))
        (enqueue-barrier) (finish)		
        (def endnanotime_bufferCreateTime (. System (nanoTime)))

        (def startnanotime_kerneltime (. System (nanoTime)))
        (loop [k InnerLoopCount]
                (do
                        (enqueue-kernel :randomnumbergen gloal_size Buffer_int1) 
                        (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
                        (finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
        (def endnanotime_kerneltime (. System (nanoTime)))
        
        (def startnanotime_bufferReadOutTime (. System (nanoTime)))
        
        (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 4096])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
    ;    (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 6400])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
    ;    (swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
 
        (def endnanotime_bufferReadOutTime (. System (nanoTime)))
        (enqueue-barrier) (finish)
        (release! Buffer_int1)
        (enqueue-barrier)
        (finish)
    nil)))

;(println (vec @OpenCLoutputAtom1))
;(println (vec @OpenCLoutputAtom2))
;(println (vec @OpenCLoutputAtom3))

(println(count @OpenCLoutputAtom1))
(println(reduce + @OpenCLoutputAtom1) )
(println(/ (reduce + @OpenCLoutputAtom1) (count @OpenCLoutputAtom1) ))

(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000.0)
           "\nnumber operations per second:" (/ (bigint(/ (* gloal_size_clj innerloop_clj) (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000000.0))) 1000000000.0) " Bln"
        ))

(nth @OpenCLoutputAtom1 0)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;pixels per world cell
(def scale 5)
(def dim 120)

(defn render [g]
  (let [img (new BufferedImage (* scale dim) (* scale dim) 
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
    (doto bg
      (.setColor (. Color white))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (doto bg
	 (.setColor (. Color blue))
	 (.drawRect 10 20 30 40)
	 (.drawLine 15 15 15 15))
	 
	 (dorun 
      (for [x (range 1000) ]
	    (doto bg
		  (.setColor (. Color red))
	      (.drawLine  (+ ( / (nth @OpenCLoutputAtom1 x) 10.0) 300)
		              x 
					  (+ ( / (nth @OpenCLoutputAtom1 x) 10.0) 300)
					  x))))
	
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))))

	
(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension 
                                     (* scale dim)
                                     (* scale dim)))))

(def frame (doto (new JFrame) (.add panel) .pack .show))





;Total OpenCL kereltime in ms: 1715.679627
;number operations per second: 11.734509685  Bln
;LudoClojure.core=> (* 64 64 64 64 12)
;201326592        
        

		
;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops26
;moving random gen to within kernel for itterative creation 
 ;;TODO see what happen with itterative random number generation (based on previous random number created
 
 
 
(def sourceOpenCL
  "



__kernel void randomnumbergen(
    __global uint *output_i,
    __global uint *output_mz
	)
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);


uint m_z;
uint m_w;

m_z = 36969 * (gid & 65535) + (gid >> 16);
m_w = 18000 * (gid & 65535) + (gid >> 16);



    output_i[gid] = ((m_z << 16) + m_w)  % 1000;
	output_mz[gid] = m_z;

}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))



;m_w = <choose-initializer>;    /* must not be zero */
;m_z = <choose-initializer>;    /* must not be zero */
; 
;uint get_random()
;{
;    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
;    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
;    return (m_z << 16) + m_w;  /* 32-bit result */
;}
(def gloal_size_clj (* 512))
(def innerloop_clj 10)

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [gloal_size gloal_size_clj
        InnerLoopCount innerloop_clj]
        
        (println "about to onload buffer")
        (def startnanotime_bufferCreateTime (. System (nanoTime)))
        (def Buffer_int1   (create-buffer gloal_size :int32  ))
		(def Buffer_int2   (create-buffer gloal_size :int32  ))
        (enqueue-barrier) (finish)		
        (def endnanotime_bufferCreateTime (. System (nanoTime)))

        (def startnanotime_kerneltime (. System (nanoTime)))
        (loop [k InnerLoopCount]
                (do
                        (enqueue-kernel :randomnumbergen gloal_size Buffer_int1 Buffer_int2) 
                        (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
                        (finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
        (def endnanotime_kerneltime (. System (nanoTime)))
        
        (def startnanotime_bufferReadOutTime (. System (nanoTime)))
        
        (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 511])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
        (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 511])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
    ;    (swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
 
        (def endnanotime_bufferReadOutTime (. System (nanoTime)))
        (enqueue-barrier) (finish)
        (release! Buffer_int1)
        (enqueue-barrier)
        (finish)
    nil)))

(println (vec @OpenCLoutputAtom1))
(println (vec @OpenCLoutputAtom2))
;(println (vec @OpenCLoutputAtom3))

(println(count @OpenCLoutputAtom1))
(println(reduce + @OpenCLoutputAtom1) )
(println(/ (reduce + @OpenCLoutputAtom1) (count @OpenCLoutputAtom1) ))

(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000.0)
           "\nnumber operations per second:" (/ (bigint(/ (* gloal_size_clj innerloop_clj) (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000000.0))) 1000000000.0) " Bln"
        ))

(nth @OpenCLoutputAtom1 0)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;pixels per world cell
(def scale 5)
(def dim 120)

(defn render [g]
  (let [img (new BufferedImage (* scale dim) (* scale dim) 
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
    (doto bg
      (.setColor (. Color white))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (doto bg
	 (.setColor (. Color blue))
	 (.drawRect 10 20 30 40)
	 (.drawLine 15 15 15 15))
	 
	 (dorun 
      (for [x (range 511) ]
	    (doto bg
		  (.setColor (. Color red))
	      (.drawLine  (+ ( / (nth @OpenCLoutputAtom1 x) 10.0) 300)
		              x 
					  (+ ( / (nth @OpenCLoutputAtom1 x) 10.0) 300)
					  x))))
	
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))
	))

	
(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension 
                                     (* scale dim)
                                     (* scale dim)))))

(def frame (doto (new JFrame) (.add panel) .pack .show))



;(. panel (repaint))

;Total OpenCL kereltime in ms: 1715.679627
;number operations per second: 11.734509685  Bln
;LudoClojure.core=> (* 64 64 64 64 12)
;201326592        
        

		
;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops27
;moving random gen to within kernel for itterative creation 
 ;;TODO see what happen with itterative random number generation (based on previous random number created: DONE: random nnumber works as intended, subsequent random numbers look really random
 
 
 
(def sourceOpenCL
  "



__kernel void randomnumbergen(
    __global uint *output_i,
    __global uint *output_mz,
	const unsigned int kernelloopsize
	)
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);

int iatom;
uint m_z = gid + 1;  //random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms
uint m_w = 1;

for( iatom = 0; iatom < kernelloopsize; iatom+=1 )
    {
    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
    m_w = 18000 * (m_w & 65535) + (m_w >> 16);

    output_i[gid*kernelloopsize + iatom ] = ((m_z << 16) + m_w)  % 10;      //This is the random number being created. Note the mod operation %, this makes it easy to create a random in some range...
	output_mz[gid*kernelloopsize + iatom ] = m_z;
	
	//Further neuron code goes within this loop. This loop is per neuron, each random number + some transform function on the random number is the proceduraly generated LSM synapse to resultand neuron pointers.
	//Will need reducer like logic... with probably with +=
	
    }
	
	
}
  ")
  
(def OpenCLoutputAtom1 (atom [1]))
(def OpenCLoutputAtom2 (atom [1]))
(def OpenCLoutputAtom3 (atom [1]))



;m_w = <choose-initializer>;    /* must not be zero */
;m_z = <choose-initializer>;    /* must not be zero */
; 
;uint get_random()
;{
;    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
;    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
;    return (m_z << 16) + m_w;  /* 32-bit result */
;}
(def gloal_size_clj (* 512))
(def kernelloopsize_clj 500)
(def innerloop_clj 10)

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [gloal_size gloal_size_clj
        InnerLoopCount innerloop_clj
		kernelloopsize kernelloopsize_clj]
        
        (println "about to onload buffer")
        (def startnanotime_bufferCreateTime (. System (nanoTime)))
        (def Buffer_int1   (create-buffer (* gloal_size kernelloopsize) :int32  ))
		(def Buffer_int2   (create-buffer (* gloal_size kernelloopsize) :int32  ))
        (enqueue-barrier) (finish)		
        (def endnanotime_bufferCreateTime (. System (nanoTime)))

        (def startnanotime_kerneltime (. System (nanoTime)))
        (loop [k InnerLoopCount]
                (do
                        (enqueue-kernel :randomnumbergen gloal_size Buffer_int1 Buffer_int2 kernelloopsize)   
                        (enqueue-barrier)   ;Could load in a huge buffer a bloack at a time...
						;;;Note, this is where out 'double buffer' will go, we'll keep swapping the state back and forth between 2 sets of buffers
                        (finish))
          (if (= k 1) nil (recur (dec k) )))					  ;;This seem like the correct time to enforce execution?!!
        (def endnanotime_kerneltime (. System (nanoTime)))
        
        (def startnanotime_bufferReadOutTime (. System (nanoTime)))
        
        (swap! OpenCLoutputAtom1 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int1 [0 511])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
        (swap! OpenCLoutputAtom2 (fn [foo] (^ints int-array (deref (enqueue-read Buffer_int2 [0 511])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
    ;    (swap! OpenCLoutputAtom3 (fn [foo] (^floats float-array (deref (enqueue-read Buffer_float [0 4])))))  ;OPTIMAL READOUT, WORNING!! this APPENDS extra data to a float-array
 
        (def endnanotime_bufferReadOutTime (. System (nanoTime)))
        (enqueue-barrier) (finish)
        (release! Buffer_int1)
        (enqueue-barrier)
        (finish)
    nil)))

(println (vec @OpenCLoutputAtom1))
(println (vec @OpenCLoutputAtom2))
;(println (vec @OpenCLoutputAtom3))

(println(count @OpenCLoutputAtom1))
(println(reduce + @OpenCLoutputAtom1) )
(println(/ (reduce + @OpenCLoutputAtom1) (count @OpenCLoutputAtom1) ))

(do (println "Total OpenCL kereltime in ms:" (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000.0)
           "\nnumber operations per second:" (/ (bigint(/ (* gloal_size_clj innerloop_clj) (/ (- endnanotime_kerneltime startnanotime_kerneltime ) 1000000000.0))) 1000000000.0) " Bln"
        ))

(nth @OpenCLoutputAtom1 0)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; UI ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;pixels per world cell
(def scale 5)
(def dim 120)

(defn render [g]
  (let [img (new BufferedImage (* scale dim) (* scale dim) 
                 (. BufferedImage TYPE_INT_ARGB))
        bg (. img (getGraphics))]
    (doto bg
      (.setColor (. Color white))
      (.fillRect 0 0 (. img (getWidth)) (. img (getHeight))))
    (doto bg
	 (.setColor (. Color blue))
	 (.drawRect 10 20 30 40)
	 (.drawLine 15 15 15 15))
	 
	 (dorun 
      (for [x (range 511) ]
	    (doto bg
		  (.setColor (. Color red))
	      (.drawLine  (+ (nth @OpenCLoutputAtom1 x) 50)
		              x 
					  (+ (nth @OpenCLoutputAtom1 x) 50)
					  x))))
	
    (. g (drawImage img 0 0 nil))
    (. bg (dispose))
	))

	
(def panel (doto (proxy [JPanel] []
                        (paint [g] (render g)))
             (.setPreferredSize (new Dimension 
                                     (* scale dim)
                                     (* scale dim)))))

(def frame (doto (new JFrame) (.add panel) .pack .show))



;(. panel (repaint))

;Total OpenCL kereltime in ms: 1715.679627
;number operations per second: 11.734509685  Bln
;LudoClojure.core=> (* 64 64 64 64 12)
;201326592        
        

		
;(serialize [12 2 3] "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach")
;Warning this gets HUGE! (* 64 64 64 32)
;(serialize (vec @OpenCLoutputAtom2) "c:/cygwin/home/Ludo/git/LudoClojure/data/scrach/OpenCLoutputAtom2data")
;(serialize boo "/home/ludo/Documents/serializationTest2.sclj")
;(def boo2 (deserialize "/home/ludo/Documents/serializationTest2.sclj"))


;;TODO 
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;OK TODO optimisation with  --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);







