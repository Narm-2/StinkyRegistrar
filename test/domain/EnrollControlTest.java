package domain;

import static org.junit.Assert.*;

import java.util.*;

import domain.exceptions.EnrollmentRulesViolationException;
import org.junit.Before;
import org.junit.Test;

public class EnrollControlTest {
	private int BASIC_UNIT = 3;

	private Student bebe;
	private Course prog;
	private Course ap;
	private Course dm;
	private Course math1;
	private Course math2;
	private Course phys1;
	private Course phys2;
	private Course maaref;
	private Course farsi;
	private Course english;
	private Course akhlagh;
	private Course economy;
	private Course karafarini;
	private Course karagah1;
	private Course karagah2;

	private List<Course> basicCourses;
	private List<Course> advancedCourses;
	private List<Course> prerequisitesCourses;
	private List<Course> smallCourses;

	@Before
	public void setup() {
		math1 = new Course("4", "MATH1", 3);
		phys1 = new Course("8", "PHYS1", 3);
		prog = new Course("7", "PROG", 3);
		prerequisitesCourses = new ArrayList<>();
		prerequisitesCourses.add(math1);
		prerequisitesCourses.add(phys1);
		prerequisitesCourses.add(prog);
		
		math2 = new Course("6", "MATH2", 3).withPre(math1);
		phys2 = new Course("9", "PHYS2", 3).withPre(math1, phys1);
		ap = new Course("2", "AP", 3).withPre(prog);
		dm = new Course("3", "DM", 3).withPre(prog);
		advancedCourses = new ArrayList<>();
		advancedCourses.add(math2);
		advancedCourses.add(phys2);
		advancedCourses.add(ap);
		advancedCourses.add(dm);
		
		economy = new Course("1", "ECO", BASIC_UNIT);
		maaref = new Course("5", "MAAREF", BASIC_UNIT);
		farsi = new Course("12", "FA", BASIC_UNIT);
		english = new Course("10", "EN", BASIC_UNIT);
		akhlagh = new Course("11", "AKHLAGH", BASIC_UNIT);
		karafarini = new Course("13", "KAR", BASIC_UNIT);
		basicCourses = new ArrayList<>();
		basicCourses.add(economy);
		basicCourses.add(maaref);
		basicCourses.add(farsi);
		basicCourses.add(english);
		basicCourses.add(akhlagh);
		basicCourses.add(karafarini);

		karagah1 = new Course("20", "KAR2", 1);
		karagah2 = new Course("21", "KAR1", 1);
		smallCourses = new ArrayList<>();
		smallCourses.add(karagah1);
		smallCourses.add(karagah2);

		bebe = new Student("1", "Bebe");
	}

	private ArrayList<Offering> requestedOfferings(List<Course> courses) {
		Calendar cal = Calendar.getInstance();
		ArrayList<Offering> result = new ArrayList<>();
		for (Course course : courses) {
			cal.add(Calendar.DATE, 1);
			result.add(new Offering(course, cal.getTime()));
		}
		return result;
	}

	private boolean hasTaken(Student s, List<Course> courses) {
	    Set<Course> coursesTaken = new HashSet<>();
		for (Offering cs : s.getCurrentTerm())
				coursesTaken.add(cs.getCourse());
		for (Course course : courses) {
			if (!coursesTaken.contains(course))
				return false;
		}
		return true;
	}

	private void addPrerequisitesTranscript(Student student, int score, int term, int CourseCount, int courseStartingIndex) {
		String strTerm = "Term" + Integer.toString(term);
		for(int i=0; i<CourseCount; i++) {
			student.addTranscriptRecord(prerequisitesCourses.get(i+courseStartingIndex), new Term(strTerm), score);
		}
	}
	
	private void addBasicTranscript(Student student, int score, int term, int CourseCount, int courseStartingIndex) {
		String strTerm = "Term" + Integer.toString(term);
		for(int i=0; i<CourseCount; i++) {
			student.addTranscriptRecord(basicCourses.get(i+courseStartingIndex), new Term(strTerm), score);
		}
	}

	private List<EnrollmentRulesViolationException> checkOfferingWithGivenUnitCountAndGPA(int requestedUnits, int gpa) {
		addPrerequisitesTranscript(bebe, gpa, 1, 1, 0);
		
		int courseCount = requestedUnits / BASIC_UNIT;
		int remainingUnits = requestedUnits % BASIC_UNIT;
	
		List<Course> requestedCourses = basicCourses.subList(0, courseCount);
		requestedCourses.addAll(smallCourses.subList(0, remainingUnits));

		List<EnrollmentRulesViolationException> exceptions =
			new EnrollControl().enroll(bebe, requestedOfferings(requestedCourses));
		if (exceptions.size() == 0)
			assertTrue(hasTaken(bebe, requestedCourses));
		return exceptions;
	}

	@Test
	public void canTakeCourses() {
		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(prerequisitesCourses));
		assertTrue(hasTaken(bebe, prerequisitesCourses));
		assertTrue(exceptions.size() == 0);
	}

	@Test
	public void canTakeNoOfferings() {
		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, new ArrayList<>());
		assertTrue(hasTaken(bebe, new ArrayList<>()));
		assertTrue(exceptions.size() == 0);
	}

	@Test
	public void cannotTakeWithoutPrerequisitesTaken() {
		List<Course> requestedCourses = Arrays.asList(basicCourses.get(0), basicCourses.get(1), advancedCourses.get(0));
		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(requestedCourses));
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void cannotTakeWithoutPrerequisitesPassed() {
		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 1, 2, 0);
		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE-1, 1, 1, 2);
		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(advancedCourses));
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void canTakeWithPrerequisitesFinallyPassed() {
		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 1, 2, 0);
		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE-1, 1, 1, 2);

		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 2, 1, 2);
		addBasicTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 2, 2, 0);

		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(advancedCourses));
		assertTrue(hasTaken(bebe, advancedCourses));
		assertTrue(exceptions.size() == 0);
	}

	@Test
	public void cannotTakeAlreadyPassedAfterFailure() {
		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE-1, 1, 1, 0);
		addBasicTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 1, 2, 0);

		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 2, 1, 0);
		addBasicTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 1, 1, 2);

		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(prerequisitesCourses));
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void cannotTakeAlreadyPassed() {
		addPrerequisitesTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 1, 1, 0);
		addBasicTranscript(bebe, EnrollControl.MIN_PASSED_SCORE+1, 1, 2, 0);

		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(prerequisitesCourses));
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void cannotTakeOfferingsWithSameExamTime() {
		Calendar cal = Calendar.getInstance();
		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe,
				List.of(
					new Offering(basicCourses.get(0), cal.getTime()),
					new Offering(basicCourses.get(1), cal.getTime()),
					new Offering(basicCourses.get(2), cal.getTime())
				));
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void cannotTakeACourseTwice() {
		List<Course> requestedCourses = Arrays.asList(basicCourses.get(0), basicCourses.get(0), basicCourses.get(1));
		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(requestedCourses));
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void UnqualifiedStudentCanTakeUnqualifiedMaxUnit() {
		List<EnrollmentRulesViolationException> exceptions = 
			checkOfferingWithGivenUnitCountAndGPA(EnrollControl.UNQUALIFIED_MAX_UNITS, EnrollControl.UNQUALIFIED_GPA_BORDER-1);
		assertTrue(exceptions.size() == 0);
	}

	@Test
	public void UnqualifiedStudentCannotTakeMoreThanUnqualifiedMaxUnit() {
		List<EnrollmentRulesViolationException> exceptions = 
			checkOfferingWithGivenUnitCountAndGPA(EnrollControl.UNQUALIFIED_MAX_UNITS+1, EnrollControl.UNQUALIFIED_GPA_BORDER-1);
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void NormalStudentCanTakeMoreThanUnqualifiedMaxUnit() {
		List<EnrollmentRulesViolationException> exceptions = 
			checkOfferingWithGivenUnitCountAndGPA(EnrollControl.UNQUALIFIED_MAX_UNITS+1, EnrollControl.UNQUALIFIED_GPA_BORDER);
		assertTrue(exceptions.size() == 0);
	}

	@Test
	public void NormalStudentCannotTakeMoreThanGeneralMaxUnit() {
		List<EnrollmentRulesViolationException> exceptions = 
			checkOfferingWithGivenUnitCountAndGPA(EnrollControl.GENERAL_MAX_UNITS+1, EnrollControl.PRIVILEGED_GPA_BORDER-1);
		assertTrue(exceptions.size() == 1);
	}

	@Test
	public void PrivilegedStudentCanTakeMaxUnits() {
		List<EnrollmentRulesViolationException> exceptions = 
			checkOfferingWithGivenUnitCountAndGPA(EnrollControl.PRIVILEGED_MAX_UNITS, EnrollControl.PRIVILEGED_GPA_BORDER);
		assertTrue(exceptions.size() == 0);
	}

	@Test
	public void cannotTakeMoreThanMaxUnits() {
		List<Course> requestedCourses = basicCourses;
		requestedCourses.add(prerequisitesCourses.get(0));
		requestedCourses.add(prerequisitesCourses.get(1));

		List<EnrollmentRulesViolationException> exceptions = 
			new EnrollControl().enroll(bebe, requestedOfferings(requestedCourses));
		assertTrue(exceptions.size() == 1);
	}
}