

;; Added my Ludo to uberTest compatibility.
  (def sourceOpenCL
  "__kernel void square (
       __global const float *a,
       __global float *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + a[gid];
  }")

(def answerIs
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
      (enqueue-read b)))))
	  
	  
;;Lets see what we can do here....
;build random float array...

(def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [(rand) (rand) (rand) (rand)] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
	  
      (enqueue-read b)))))
	  
(deref answerIs2)
	  
(vec (repeat 4 (rand)))
(def RandomFloatVec (vec (for [i (range 100)] (rand))))

(def answerIs3
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap (vec (repeat 100 (rand))) :float32)
          b (mimic a)]
      (enqueue-kernel :square 100 a b)
      (enqueue-read b)))))


(def RandomFloatVec (vec (for [i (range 100)] (rand))))
(def answerIs4
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap RandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 100 a b)
      (enqueue-read b)
		 ))))
		 
(count @answerIs4)
(println (count @answerIs4))
;(release! b)


;;Ram Test...how big can we get?  This works but seems wrong as release! does not work... (can not run twise without running out of vRAM)
(def largelist (for [i (range 50000)] (rand)))
(def LRandomFloatVec (vec largelist))
(time (def answerIs5
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LRandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 50000 a b)
      
	  (def AnswerAtomizedBackToMainRam5 (atom @(enqueue-read b)))
	  (release! a)  ;;This does not work... what is a 'buffer'?
	  (release! b)
	  ;;(released? a)
	  )))))
(count @AnswerAtomizedBackToMainRam5)
(nth @AnswerAtomizedBackToMainRam5 1)


;;example6
(def largelist (for [i (range 25000)] (rand)))
(def LaRandomFloatVec (vec largelist))
(def AnswerAtomizedBackToMainRam6 (atom []))

(def AnswerAtomizedBackToMainRam6
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 25000 a b)
      (enqueue-read b)
	  ;(swap! AnswerAtomizedBackToMainRam6 (fn [_] @(enqueue-read b)))
	  ;(release! a)  ;;This does not work... what is a 'buffer'?
	  ;(release! b)
	  ;;(released? a)
	  ))))

(def LaRandomFloatVec (vec largelist))
	  
(println (count @AnswerAtomizedBackToMainRam6))
(println (nth LaRandomFloatVec 1))
(println "Answer from CPU: " (* (nth LaRandomFloatVec 1) (nth LaRandomFloatVec 1)))
(println "Answer from GPU: " (nth @AnswerAtomizedBackToMainRam6 1))
;(println (nth LaRandomFloatVec 2499999))
;(println "Answer from CPU Last: " (* (nth LaRandomFloatVec 2499999) (nth LaRandomFloatVec 2499999)))
;(println "Answer from GPU:Last: " (nth @AnswerAtomizedBackToMainRam6 2499999))



(defn AnswerAtomizedBackToMainRam7 []
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (enqueue-kernel :square 2500000 a b)
      (enqueue-read b)
	  ))))
;;;NOTE looking at AnswerAtomizedBackToMainRam7 directly will TAKE ALL RAM and NOT RELEASE.
;;; @AnswerAtomizedBackToMainRam7 will require output is looked at, but will then allow GC to clean up ram..


(def largelist (for [i (range 2500000)] (rand)))
(def LaRandomFloatVec (vec largelist))	  
(println "Answer from CPU: " (* (nth LaRandomFloatVec 1) (nth LaRandomFloatVec 1)))
(println "Answer from GPU: " (nth @(AnswerAtomizedBackToMainRam7) 1))

(def HeldOutputEnqueueReadHolder7 (AnswerAtomizedBackToMainRam7))
(defn HeldOutputEnqueueReadHolder_read []
     (nth @HeldOutputEnqueueReadHolder7 1))
(HeldOutputEnqueueReadHolder_read)    ;Only Reading releases the RAM held by the (with-cl...(enqueue-read b)) block, becuase the enqueue-read 'stackes' the read queue up.,,,
	 
	 
(time (doseq [i (range 1 2)]  @(AnswerAtomizedBackToMainRam7)))





(def sourceOpenCL
  "__kernel void square (
       __global const float *a,
       __global float *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] * a[gid];
  }")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  
;;;;; Iteration loops1
(def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1 2 3 5] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 b a)
	  (enqueue-kernel :square 4 a b)
      (enqueue-read b)))))
	  
(deref answerIs2)
;;;;; Iteration loops2
(def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.1 1.2 1.3 1.4] :float32)
          b (mimic a)]
      (enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 b a)
	  (enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 b a)
      (enqueue-read b)))))
	  
(deref answerIs2)

;;;;; Iteration loops3
(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.0000000 1.00000000 1.0000000 1.00000000] :float32)
          b (mimic a)]
      (dorun (map (fn [x] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 4 b a) (println "done:" x))   (range 1 10)))
      (enqueue-read b))))))
	  
(deref answerIs2)

;;;;; Iteration loops4
(quote 
(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.0000000 1.00000000 1.0000000 1.00000000] :float32)
          b (mimic a)]
      (dorun (map (fn [x] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 4 b a))   (range 1 100000)))
      ;(enqueue-read b)
	  )))))
	  
(deref answerIs2))

;;;;; Iteration loops4
(quote 
(def largelist (for [i (range 25000)] 1.0))
(def LaRandomFloatVec (vec largelist))

(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (dorun (map (fn [x] (enqueue-kernel :square 4 a b) (enqueue-kernel :square 25000 b a))   (range 1 100000)))
      ;(enqueue-read b)
	  )))))
)
;;;;;;  26secs for 100000 *2 FLOPS over 25000 vector... or 192,000,000 operations per second...
(deref answerIs2)


;;;;; Iteration loops4
(quote 
(def largelist (for [i (range 500000)] 1.0))
(def LaRandomFloatVec (vec largelist))

(time (def answerIs2
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      (dorun 100 (fn [] (enqueue-kernel :square 5000000 a b) (enqueue-kernel :square 500000 b a)) )
      ;(enqueue-read b)
	  (release! a)
	  (release! b)
	  )))))
(count (deref answerIs2) )
	  
)
;;;;;;  26.0secs for 100000  *2 FLOPS over   25000 vector... or    192,000,000 operations per second...
;;;;;;   2.2sec  for     10  *2             500000           or     10,000,000 per second
;;;;;;  35.0sec      1000000 *2 FLOPS over  500000 vector    or  BOO2,850,000,000 per second
(deref answerIs2)

;(dorun (repeatedly 100 (fn [] (println "fpp")  )))

;;;;; Iteration loops5
(quote 
  (def sourceOpenCL
  "__kernel void square (
       __global float *a,
       __global float *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + 1;
  }")

(def largelist (for [i (range 2000000)] 1))
(def LaRandomFloatVec (vec largelist))


(time (dorun  0 (repeatedly (fn []     ;;--massive costs of  openCL bootup, 160ms or so!
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :float32)
          b (mimic a)]
      ;(dorun 200000 (repeatedly (fn [] (enqueue-kernel :square 200 a b) (enqueue-kernel :square 200 b a))  ))
	  ;(dorun 200000 (repeatedly (fn [] (enqueue-kernel :square 200 a b) (enqueue-kernel :square 200 b a))  ))    ;;Why does this eat ram????
	   (dorun 10 (repeatedly
	                          (fn [] (enqueue-kernel :square 2000000 a b) (enqueue-kernel :square 2000000 b a))  ))
	  ;;Trying loop recur...
	  
	  (enqueue-kernel :square 2000000 a b)
	  (release! a)
	  (release! b)
      (def answerIs2b (enqueue-read b))
	  (def answerIs2a (enqueue-read a))
	  ;(count (deref answerIs2b) )
	  nil
	  )))
))))

(count (deref answerIs2b) )
(* 2 (nth LaRandomFloatVec 1))
(nth (deref answerIs2b) 1)
(nth (deref answerIs2a) 1)

;;;; 2000 FLOPS * 2000000 items in 0.757 -> (/ (* 2000 2000000) 0.757) -> 5,284,000,000
;;;; 2000 FLOPS * 2000000 items in 0.757 -> (/ (* 2000 2000000) 0.757) -> 5,284,000,000


(def outputInCLJ_Ram (deref answerIs2))
(nth outputInCLJ_Ram  1)    ;Not that accessing the ref on the graphics card is SLOW
)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops6
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(def largelist (for [i (range 1000000)] 1))
(def LaRandomFloatVec (vec largelist))


(def myFooAtom1 (atom 0))
(def myFooAtom2 (atom 0))
(time 
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :int32)
          b (mimic a)]
	  
	  (time (dorun 1000 (repeatedly(fn []
	           
						(wait-for (enqueue-kernel :square 1000000 a b))
						(wait-for (enqueue-kernel :square 1000000 b a))
						))))
	  (release! a)
	  (release! b)
	  (time (swap! myFooAtom2 (fn [x] (deref (enqueue-read b)))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil
	  )))
)


(count (deref myFooAtom2) )
(time (nth (deref myFooAtom2) 1))
(time (count (deref myFooAtom2)))
(time (reduce + (deref  myFooAtom2)))

(count (deref (deref myFooAtom2)))
(swap! myFooAtom2 (fn [x] 1))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops7
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(defn buildDatatoWorkWith [sizeOfarrayf]
 (def largelist (for [i (range sizeOfarrayf)] 1))
 (def LaRandomFloatVec (vec largelist))

 (def myFooAtom1 (atom 0))
 (def myFooAtom2 (atom 0)))

(defn RunOneOpenCL_uberloop [InnerLoopCount sizeOfarray] 
(println "starting")
(time 
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :int32)
          b (mimic a)]
	  
	  (time (dorun InnerLoopCount (repeatedly(fn []
	           
						(wait-for (enqueue-kernel :square sizeOfarray a b))
						(wait-for (enqueue-kernel :square sizeOfarray b a))
						))))     ;have feeling this is stack overflowing... need to use loop recur?
	  (release! a)
	  (release! b)
	  ;(time (swap! myFooAtom2 (fn [x] (deref (enqueue-read b)))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil
	  )))
)
)  ;;RunOneOpenCL_uberloop
;(RunOneOpenCL_uberloop)

(defn SafeLoop [n functionToCall Isize asize]
  (loop [k n]
    (functionToCall Isize asize)
    (if (= k 1)
      (println "Done Work")
      (recur (dec k) ))))

(def InnerLoopCount 100)
(def sizeOfarray 1000000)
(buildDatatoWorkWith sizeOfarray)
(SafeLoop 50 RunOneOpenCL_uberloop InnerLoopCount sizeOfarray)

;Benchmarks
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 1.4))) 
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 160))) 


(count LaRandomFloatVec)
(count (deref myFooAtom2) )
(time (nth (deref myFooAtom2) 1))
(time (count (deref myFooAtom2)))
(time (reduce + (deref  myFooAtom2)))

(count (deref (deref myFooAtom2)))
(swap! myFooAtom2 (fn [x] 1))
)





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops8
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global int *a,
       __global int *b) {
    int gid = get_global_id(0);
    b[gid] = a[gid] + gid;
  }")

(defn buildDatatoWorkWith [sizeOfarrayf]
 (def largelist (for [i (range sizeOfarrayf)] 1))
 (def LaRandomFloatVec (vec largelist))
 (def LaRandomFloatVecAtom (atom LaRandomFloatVec))
 (def myFooAtom1 (atom 0))
 (def myFooAtom2 (atom 0)))

 
 
(defn RunOneOpenCL_uberloop [InnerLoopCount sizeOfarray] 
 (println "starting")
 (time 
 (with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap LaRandomFloatVec :int32)
          b (mimic a)]
	  
	  (time   (loop [k InnerLoopCount]
						;(wait-for (enqueue-kernel :square sizeOfarray (wrap @LaRandomFloatVecAtom :int32) b))   ;;DEMO OF LOADING IN AN ARRAY FROM THE 'OUTSIDE', this could be in a (let []...) that would cover a whole block
						(wait-for (enqueue-kernel :square sizeOfarray a b))
						(enqueue-barrier)
						(wait-for (enqueue-kernel :square sizeOfarray b a))
						(enqueue-barrier)
						
                (if (= k 1)
				
                nil
                (recur (dec k) ))))     ;have feeling this is stack overflowing... need to use loop recur?
	  (release! a)
	  (release! b)	  
	  ;(swap! myFooAtom2 (fn [x] (enqueue-read b)))
	  ;;(time (swap! myFooAtom2 (fn [x] (enqueue-read b))))    ;;(enqueue-read b) NEEDS to be deref'ed to release memory...
	  nil)))))
;;RunOneOpenCL_uberloop
;(RunOneOpenCL_uberloop)

(defn SafeLoop [n functionToCall Isize asize]
  (loop [k n]
    (functionToCall Isize asize)
    (if (= k 1)
      (println "Done Work")
      (recur (dec k) ))))

(def InnerLoopCount 5000)
(def sizeOfarray 10000000)
(buildDatatoWorkWith sizeOfarray)
(SafeLoop 10 RunOneOpenCL_uberloop InnerLoopCount sizeOfarray)

;Benchmarks
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (bigint (/ (* InnerLoopCount sizeOfarray 2) 1.4))) 
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 160) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi
(println "InnerLoops:" InnerLoopCount " SizeOfArray: "sizeOfarray  " OpsPerSecond was: " (/ (bigint (/ (* InnerLoopCount sizeOfarray 2) 14) ) 1000000000.0) "Billion")  ;5.8B , 6.25Bi , 7.1B 10MArray




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops9
(quote 

(def sourceOpenCL
  "__kernel void square (
       __global float *a,
       __global float *b
	   ) {
    int gid = get_global_id(0);
	float foo = 1.0;
    b[gid] = a[gid] + foo;
  }")


(def ArrayToPass (atom [12.0 34.0 45.0 67.0]))

(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
      ;(enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 (wrap @ArrayToPass :float32) b)
      (enqueue-read b))))
	  
(with-cl
  (with-program (compile-program sourceOpenCL)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
      ;(enqueue-kernel :square 4 a b)
	  (enqueue-kernel :square 4 a b)
      (enqueue-read b))))


	  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops10    -meny kernels?   : OK for many kernels as per this demo
(quote 
  
(def sourceOpenCL2
  "
  __kernel void square (
       __global float *a,
       __global float *b
	   ) {
    int gid = get_global_id(0);
	float foo = 1.0;
    b[gid] = a[gid] + foo;
  }

  __kernel void square2 (
       __global float *a,
       __global float *b
	   ) {
    int gid = get_global_id(0);
	float foo = 2.0;
    b[gid] = a[gid] * foo;
  }
  ")

(def ArrayToPass (atom [12.0 34.0 45.0 67.0]))

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap [1.011 11 12 5] :float32)
          b (mimic a)]
	  (enqueue-kernel :square 4 (wrap @ArrayToPass :float32) b)
	  (enqueue-kernel :square 4 b a)
	  ;; sequential work makes the buffer persists, but barriers are likely needed for paralelisim safety. 
      (enqueue-read a))))
	    
)
		
		
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops11    -may buffers, postional access based on ints in another buffer
;;Difficult getting a single int out... maybe an array of floats denoting different outputs?  OK!!
;;NOTE: any mixing of datatypes can be silently catastrophic!
;;NOTE: this is very inneficent (positonvalout array outputs are computed 4 times for the same set of numbers...)
(quote 

(def sourceOpenCL2
  "
  __kernel void testingbuffers (
       __global float *a,
       __global float *b,
	   __global int *inta,
	   int positon,
	   __global int *positonvalout
	   ) {
    int gid = get_global_id(0);
	int lsize = get_local_size(0);  
	int lid = get_local_id(0);
	
	float foo = 1.0;
    b[gid] = a[gid] + foo;
	positonvalout[0] =  inta[positon];
	positonvalout[1]  =  inta[0] + inta[1] + 5;
	positonvalout[2]  = gid ;
	positonvalout[3]  = lsize ;
	positonvalout[4]  = lid;
  }
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap [1.011 11.5 12.4 5.0001] :float32)
          b (mimic a)
		  inta (wrap [8 7 3 4 7 3 1 2] :int32)
		  positon 0
		  positonvalout (wrap [0 0 0 0 0 0 0] :int32)
		  ]
      (enqueue-kernel :testingbuffers 4 a b inta positon positonvalout)
      (swap! OpenCLoutputAtom1 (fn [x] (deref (enqueue-read b))))
	  (swap! OpenCLoutputAtom2 (fn [x] (deref (enqueue-read positonvalout))))
	nil)))
@OpenCLoutputAtom1
@OpenCLoutputAtom2

)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops12    -CL like itteration within kernel, interkernel 'spin', 
(quote 

(def sourceOpenCL2
  "
  __kernel void testingbuffers (
       __global float *a,
       __global float *b,
	   __global int *inta,
	   int positon,
	   __global int *positonvalout
	   ) {
    int gid = get_global_id(0);
	int lsize = get_local_size(0);  
	int lid = get_local_id(0);
	
	float foo = 1.0;
    b[gid] = a[gid] + foo;
	positonvalout[0] =  inta[positon];
	positonvalout[1]  =  inta[0] + inta[1] + 5;
	positonvalout[2]  = gid ;
	positonvalout[3]  = lsize ;
	positonvalout[4]  = lid;
  }
  ")
  
(def OpenCLoutputAtom1 (atom 0))
(def OpenCLoutputAtom2 (atom 0))

(with-cl
  (with-program (compile-program sourceOpenCL2)
    (let [a (wrap [1.011 11.5 12.4 5.0001] :float32)
          b (mimic a)
		  inta (wrap [8 7 3 4 7 3 1 2] :int32)
		  positon 0
		  positonvalout (wrap [0 0 0 0 0 0 0] :int32)
		  ]
      (enqueue-kernel :testingbuffers 4 a b inta positon positonvalout)
      (swap! OpenCLoutputAtom1 (fn [x] (deref (enqueue-read b))))
	  (swap! OpenCLoutputAtom2 (fn [x] (deref (enqueue-read positonvalout))))
	nil)))
@OpenCLoutputAtom1
@OpenCLoutputAtom2

	  
;;TODO 
;OK Test may kernels
;OK many buffers
;OK passing buffers between kernels 
;OK lookup int position in one buffer to find value in another buffer
;iteration within a kernel (C like loops)
;logicals within a kernel  (C if statements?)
;;TODO optimisation with 	 --  int lsize = get_local_size(0);  --  int lid = get_local_id(0);







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

(defn -main [& args]
  (println "the following output will be out of openCL"  @answerIs))
  
;(defn main [& args]
;  (println "whats with this error huh?"  @answerIs))
  
;;;
