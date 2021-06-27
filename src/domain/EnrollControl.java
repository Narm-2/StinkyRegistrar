package domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import domain.exceptions.EnrollmentRulesViolationException;

public class EnrollControl {
	public void enroll(Student s, List<Offering> offerings) throws EnrollmentRulesViolationException {
        checkNotPassedCoursesBefore(s, offerings);
        checkPassedPrerequisites(s, offerings);
        checkExamTimeCollisions(offerings);
        checkTwiceCourseTakings(offerings);
        checkUnitsCountLimits(s, offerings);
        for (Offering o : offerings)
			s.takeCourse(o.getCourse(), o.getSection());
	}

    private void checkUnitsCountLimits(Student s, List<Offering> offerings) throws EnrollmentRulesViolationException {
        int unitsRequested = 0;
        for (Offering o : offerings)
            unitsRequested += o.getCourse().getUnits();
        double gpa = s.getGPA();
        if (checkUnitLimitationUnqualified(gpa, unitsRequested) ||
            checkUnitLimitationPrivileged(gpa, unitsRequested) ||
            checkUnitLimitationMaxUnits(unitsRequested))
            throw new EnrollmentRulesViolationException(String.format("Number of units (%d) requested does not match GPA of %f", unitsRequested, gpa));
    }

    private void checkTwiceCourseTakings(List<Offering> offerings) throws EnrollmentRulesViolationException {
        for (Offering o : offerings) {
            for (Offering o2 : offerings) {
                if (o == o2)
                    continue;
                if (o.getCourse().equals(o2.getCourse()))
                    throw new EnrollmentRulesViolationException(String.format("%s is requested to be taken twice", o.getCourse().getName()));
            }
        }
    }

    private void checkExamTimeCollisions(List<Offering> offerings) throws EnrollmentRulesViolationException {
        for (Offering o : offerings) {
            for (Offering o2 : offerings) {
                if (o == o2)
                    continue;
                if (o.getExamTime().equals(o2.getExamTime()))
                    throw new EnrollmentRulesViolationException(String.format("Two offerings %s and %s have the same exam time", o, o2));
            }
		}
    }

    private void checkNotPassedCoursesBefore(Student s, List<Offering> offerings) throws EnrollmentRulesViolationException {
        Map<Term, Map<Course, Double>> transcript = s.getTranscript();
        for (Offering o : offerings) {
            for (Map.Entry<Term, Map<Course, Double>> tr : transcript.entrySet()) {
                for (Map.Entry<Course, Double> r : tr.getValue().entrySet()) {
                    if (r.getKey().equals(o.getCourse()) && r.getValue() >= 10)
                        throw new EnrollmentRulesViolationException(String.format("The student has already passed %s", o.getCourse().getName()));
                }
            }
        }
    }

    private void checkPassedPrerequisites(Student s, List<Offering> offerings) throws EnrollmentRulesViolationException {
        
        Map<Term, Map<Course, Double>> transcript = s.getTranscript();
        Set<Course> passedCourses = new HashSet<Course>();
        for (Map.Entry<Term, Map<Course, Double>> tr : transcript.entrySet()) {
            for (Map.Entry<Course, Double> r : tr.getValue().entrySet()) {
                Course c = r.getKey();
                Double score = r.getValue();
                if (score >= 10)
                    passedCourses.add(c);
            }
        }

        for (Offering o : offerings) {
            List<Course> prerequisites = o.getCourse().getPrerequisites();
            for (Course pre : prerequisites) {
                if (!passedCourses.contains(pre))
                    throw new EnrollmentRulesViolationException(
                        String.format("The student has not passed %s as a prerequisite of %s", pre.getName(), o.getCourse().getName()));
            }
        }
    }

    private boolean checkUnitLimitationUnqualified(double gpa, int unitsRequested) {
        return (gpa < 12 && unitsRequested > 14);
    }

    private boolean checkUnitLimitationPrivileged(double gpa, int unitsRequested) {
        return (gpa < 16 && unitsRequested > 16);
    }

    private boolean checkUnitLimitationMaxUnits(int unitsRequested) {
        return (unitsRequested > 20);
    }
}
