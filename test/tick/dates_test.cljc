;; Copyright © 2016-2018, JUXT LTD.

(ns tick.dates-test
  (:refer-clojure :exclude [dec < range <= min long int > extend - time / >= inc + max complement atom swap-vals! reset-vals! compare-and-set! reset! swap! second group-by conj])
  (:require [clojure.test
             :refer [deftest is testing run-tests]
             :refer-macros [deftest is testing run-tests]]
            [tick.timezone]
            [tick.protocols :as p]
            [tick.core :as t :refer [with-clock] :refer-macros [with-clock]]))

;; See doc/dates.adoc

(deftest time-construction-test
  (testing "(time)"
    (is (t/time? (t/time))))
  (testing "(time \"4pm\")"
    (is (t/time? (p/parse "4pm")))
    (is (= "16:00" (str (p/parse "4pm")))))
  (testing "(midnight)"
    (is (t/time? (t/midnight)))
    (is (= "00:00" (str (t/midnight)))))
  (testing "(noon)"
    (is (t/time? (t/noon)))
    (is (= "12:00" (str (t/noon))))))

(deftest date-construction-test
  (is (t/date-time? (t/noon (t/today))))
  (with-clock (-> (t/date "2018-02-14") (t/at "10:00"))
    (testing "(noon (today))"
      (is (= "2018-02-14T12:00" (str (t/noon (t/today))))))
    (testing "(noon (date))"
      (is (= "2018-02-14T12:00" (str (t/noon (t/date))))))))

;; TODO: Clock tests
;; Create with a value for a fixed clock. Value can be a time or a zone

(deftest clock-test
  (testing "clock"
    (with-clock (-> (t/date "2018-02-14") (t/at "10:00") (t/in "America/New_York"))
      (testing "(clock) return type"
        (is (t/clock? (t/clock))))
      (testing "Time shifting the clock back by 2 hours"
        (is (= "2018-02-14T13:00:00Z" (str (t/instant (t/<< (t/clock) (t/new-duration 2 :hours)))))))
      (testing "with instant"
        (is (= (t/zone (t/clock (t/instant)))
              (t/zone "America/New_York"))))))

  (testing "Converting using with-clock"
    (t/with-clock (t/clock (t/zone "America/New_York"))
      (testing "inst to zoned-date-time"
        (is (= (t/zoned-date-time #inst"2019-08-07T16:00")
              (t/zoned-date-time "2019-08-07T12:00-04:00[America/New_York]"))))
      (testing "date-time to zoned-date-time"
        (is (= (t/zoned-date-time (t/date-time "2019-08-07T12:00"))
              (t/zoned-date-time "2019-08-07T12:00-04:00[America/New_York]"))))
      (testing "date-time to offset-date-time"
        (is (= (t/offset-date-time (t/date-time "2019-08-07T12:00"))
              (t/offset-date-time "2019-08-07T12:00-04:00"))))))

  (testing "Creating a clock with a zone, and returning that zone"
    (is (= "America/New_York" (str (t/zone (t/clock (t/zone "America/New_York")))))))

  (testing "Creation of clock with fixed instant"
    (is (= "2017-10-31T16:00:00Z" (str (t/instant (t/clock "2017-10-31T16:00:00Z")))))))

(deftest zdt-and-instant-equality-test
  (testing "ZonedDateTimes in different zones should be equals"
    (is (p/=
      (t/zoned-date-time "2017-10-31T16:00:00-04:00[America/New_York]")
      (t/zoned-date-time "2017-10-31T13:00:00-07:00[America/Los_Angeles]"))))

  (testing "ZoneDateTimes and OffsetDateTime should be equals if represents the same point in time"
    (is (p/=
      (t/zoned-date-time "2017-10-31T16:00:00-04:00[America/New_York]")
      (t/offset-date-time "2017-10-31T13:00-07:00"))))

  (testing "ZoneDateTimes and platform Date should be equals if represents the same point in time"
    (is (p/=
      (t/zoned-date-time "2017-10-31T16:00:00-04:00[America/New_York]")
      (t/inst "2017-10-31T20:00:00Z"))))

  (testing "Instants and ZonedDateTimes should be equals if represents the same point in time"
    (is (p/=
            (t/instant (t/clock "2017-10-31T16:00:00Z"))
            (t/zoned-date-time "2017-10-31T16:00:00Z[UTC]")))))

;; TODO: tick function

;; TODO: Atomic clocks