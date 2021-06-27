package domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import domain.exceptions.EnrollmentRulesViolationException;

public class EnrollControl {
    public static final int MIN_PASSED_SCORE = 10;

    public static final int UNQUALIFIED_GPA_BORDER = 12;
    public static final int UNQUALIFIED_MAX_UNITS = 14;
    public static final int PRIVILEGED_GPA_BORDER = 16;
    public static final int GENERAL_MAX_UNITS = 16;
    public static final int PRIVILEGED_MAX_UNITS = 20;

	public void enroll(Student student, List<Offering> offerings) throws EnrollmentRulesViolationException {
        checkNotPassedCoursesBefore(student, offerings);
        checkPassedPrerequisites(student, offerings);
        checkExamTimeCollisions(offerings);
        checkTwiceCourseTakings(offerings);
        checkUnitsCountLimits(student, offerings);
        for (Offering offer : offerings)
			student.takeCourse(offer.getCourse(), offer.getSection());
	}

    private void checkUnitsCountLimits(Student student, List<Offering> offerings) throws EnrollmentRulesViolationException {
        int unitsRequested = 0;
        for (Offering offer : offerings)
            unitsRequested += offer.getCourse().getUnits();
        double gpa = student.getGPA();
        if (checkUnitLimitationUnqualified(gpa, unitsRequested) ||
            checkUnitLimitationPrivileged(gpa, unitsRequested) ||
            checkUnitLimitationMaxUnits(unitsRequested))
            throw new EnrollmentRulesViolationException(String.format("Number of units (%d) requested does not match GPA of %f", unitsRequested, gpa));
    }

    private void checkTwiceCourseTakings(List<Offering> offerings) throws EnrollmentRulesViolationException {
        for (Offering offer : offerings) {
            for (Offering otherOffer : offerings) {
                if (offer == otherOffer)
                    continue;
                if (offer.getCourse().equals(otherOffer.getCourse()))
                    throw new EnrollmentRulesViolationException(String.format("%s is requested to be taken twice", offer.getCourse().getName()));
            }
        }
    }

    private void checkExamTimeCollisions(List<Offering> offerings) throws EnrollmentRulesViolationException {
        for (Offering offer : offerings) {
            for (Offering otherOffer : offerings) {
                if (offer == otherOffer)
                    continue;
                if (offer.getExamTime().equals(otherOffer.getExamTime()))
                    throw new EnrollmentRulesViolationException(String.format("Two offerings %s and %s have the same exam time", offer, otherOffer));
            }
		}
    }

    private void checkNotPassedCoursesBefore(Student student, List<Offering> offerings) throws EnrollmentRulesViolationException {
        Map<Term, Map<Course, Double>> transcript = student.getTranscript();
        for (Offering offer : offerings) {
            for (Map.Entry<Term, Map<Course, Double>> termTranscript : transcript.entrySet()) {
                for (Map.Entry<Course, Double> courseScore : termTranscript.getValue().entrySet()) {
                    Course course = courseScore.getKey();
                    Double score = courseScore.getValue();
                    if (course.equals(offer.getCourse()) && score >= MIN_PASSED_SCORE)
                        throw new EnrollmentRulesViolationException(String.format("The student has already passed %s", offer.getCourse().getName()));
                }
            }
        }
    }

    private void checkPassedPrerequisites(Student student, List<Offering> offerings) throws EnrollmentRulesViolationException {
        
        Map<Term, Map<Course, Double>> transcript = student.getTranscript();
        Set<Course> passedCourses = new HashSet<Course>();
        for (Map.Entry<Term, Map<Course, Double>> termTranscript : transcript.entrySet()) {
            for (Map.Entry<Course, Double> courseScore : termTranscript.getValue().entrySet()) {
                Course course = courseScore.getKey();
                Double score = courseScore.getValue();
                if (score >= MIN_PASSED_SCORE)
                    passedCourses.add(course);
            }
        }

        for (Offering offer : offerings) {
            List<Course> prerequisites = offer.getCourse().getPrerequisites();
            for (Course pre : prerequisites) {
                if (!passedCourses.contains(pre))
                    throw new EnrollmentRulesViolationException(
                        String.format("The student has not passed %s as a prerequisite of %s", pre.getName(), offer.getCourse().getName()));
            }
        }
    }

    private boolean checkUnitLimitationUnqualified(double gpa, int unitsRequested) {
        return (gpa < UNQUALIFIED_GPA_BORDER && unitsRequested > UNQUALIFIED_MAX_UNITS);
    }

    private boolean checkUnitLimitationPrivileged(double gpa, int unitsRequested) {
        return (gpa < PRIVILEGED_GPA_BORDER && unitsRequested > GENERAL_MAX_UNITS);
    }

    private boolean checkUnitLimitationMaxUnits(int unitsRequested) {
        return (unitsRequested > PRIVILEGED_MAX_UNITS);
    }
}
