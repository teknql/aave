(ns aave.syntax.ghostwheel-test
  (:require [aave.syntax.ghostwheel :as sut]
            #?(:clj [clojure.test :as t :refer [deftest testing is]]
               :cljs [cljs.test :as t :include-macros true])))


(deftest merge-param-schemas-test
  (is (= [:multi {:dispatch count}
          [0 [:tuple]]
          [1 [:tuple int?]]
          [2 [:tuple int? int?]]]
         (sut/merge-param-schemas [[:tuple]
                                   [:tuple int?]
                                   [:tuple int? int?]]))))

(deftest parse-test
  (testing "simple function"
    (let [result (sut/parse '[foo [] (+ 1 2)])]
      (is (= 'foo (:name result)))
      (is (= '([] (+ 1 2)) (:params+body result)))))

  (testing "function + meta + docs + schema"
    (let [result (sut/parse
                  '[foo "Docs" {:meta true} [x y] [int? int? => int?] (+ x y)])]
      (is (= 'foo (:name result)))
      (is (= "Docs" (:doc result)))
      (is (= '{:meta true} (:meta-map result)))
      (is (= '([x y] (+ x y)) (:params+body result)))
      (is (= '[:tuple int? int?] (:param-schema result)))
      (is (= 'int? (:ret-schema result)))))

  (testing "overloading"
    (let [result (sut/parse
                  '[foo
                    ([x] [int? => int?] (inc x))
                    ([x y] [int? int? => int?] (+ x y))])]
      (is (= 'foo (:name result)))
      (is (= '(([x] (inc x))
               ([x y] (+ x y)))
             (:params+body result)))
      (is (= [:multi {:dispatch count}
              [1  [:tuple 'int?]]
              [2 [:tuple 'int? 'int?]]]
             (:param-schema result))))))
