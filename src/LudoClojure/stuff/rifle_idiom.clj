(ns LudoClojure.rifle-idiom)


;;TODO see if this rifle idiom will do it for me....
;;This demonstrates an functional objectiviation over an atom, keeping its state. This is a closure (??) , but only if the atom is never exposed directly

(defn make-counter [init-val] 
    (let [foo (atom init-val)] 
      {:next #(swap! foo inc)
       :addcusom (fn [x] (swap! foo (fn [foo] (+ foo x))))
       :reset #(reset! foo init-val)
       :return_original_c @foo
       :return_current_catom (fn [] foo)     ;This makes it a non closure (??) beucase the state inside is now accessible via the atom, so the atom is exposed and can be changed....
       ;  :return_current_catom2  #(foo)   ;;;fails, because #(x) macro expands to (fn [] (x)) not (fn [] x)
       :return_current_catom3  (fn [] @foo)
       }))
(def c (make-counter 100))
((c :next))
((c :addcusom) 20)
((c :next))
((c :reset))
(c :returnval)          ;; 
(c :return_original_c)  ;; since foo within function definiton got derefed during defintion, this value will persists.
@((c :return_current_catom))
;  ((c :return_current_catom2))   ;;fails....  
((c :return_current_catom3))

((c :return_current_catom))
(swap! ((c :return_current_catom)) inc)