(ns legalsecretary.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [legalsecretary.actor :as actor]
            [legalsecretary.advisor :as advisor]
            [legalsecretary.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-attorney! st {:attorney-id "attorney-1" :name "Kobo Law"})
    (store/register-case! st {:case-id "C-1" :attorney-id "attorney-1"
                              :name "case-042"
                              :max-supply-cost 500})
    st))

(deftest commits-a-verified-document-log
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:attorney-id "attorney-1" :op :log-document-record :stake :low
                 :case-id "C-1" :document-id "D-1" :document-type :filed-motion}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "attorney-1"))))))

(deftest holds-a-deadline-not-attorney-supplied-schedule-proposal
  (testing "a filing-deadline decision the attorney did not actually supply
            is exactly the unsupervised legal-strategy decision this actor
            must never make -- HARD block, not merely escalate"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:attorney-id "attorney-1" :op :schedule-court-operation :stake :low
                   :case-id "C-1" :operation-date "2026-08-01" :attorney-supplied? false}
          result (actor/run-request! graph request {} "thread-2")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "attorney-1"))))))

(deftest holds-an-unknown-op-proposal
  (testing "an op outside the closed allowlist (e.g. one that would directly
            finalize disclosure of privileged case information) is a
            permanent, non-overridable block"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st
                                     :advisor (reify advisor/Advisor
                                                (-advise [_ _store _request]
                                                  {:op :finalize-privileged-disclosure
                                                   :effect :propose :case-id "C-1"
                                                   :confidence 0.99 :stake :low
                                                   :rationale "attempted disclosure to opposing counsel"}))})
          request {:attorney-id "attorney-1" :op :finalize-privileged-disclosure :stake :low
                   :case-id "C-1"}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "attorney-1"))))))

(deftest interrupts-then-approves-flag-confidentiality-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:attorney-id "attorney-1" :op :flag-confidentiality-concern :stake :low
                 :case-id "C-1"
                 :concern-detail "possible attorney-client privilege exposure: opposing counsel copied on an internal memo"}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "attorney-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "attorney-1")))))))

(deftest interrupts-then-approves-over-cost-supply-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:attorney-id "attorney-1" :op :coordinate-supply-order :stake :low
                 :case-id "C-1" :cost 5000}
        interrupted (actor/run-request! graph request {} "thread-5")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "attorney-1")))
    (let [resumed (actor/approve! graph "thread-5")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "attorney-1")))))))
