/*
 * Created on 6.1.2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.idega.block.survey.business;

import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import com.idega.block.category.data.InformationFolder;
import com.idega.block.survey.data.SurveyAnswer;
import com.idega.block.survey.data.SurveyAnswerHome;
import com.idega.block.survey.data.SurveyEntity;
import com.idega.block.survey.data.SurveyEntityHome;
import com.idega.block.survey.data.SurveyParticipant;
import com.idega.block.survey.data.SurveyParticipantHome;
import com.idega.block.survey.data.SurveyQuestion;
import com.idega.block.survey.data.SurveyQuestionHome;
import com.idega.block.survey.data.SurveyReply;
import com.idega.block.survey.data.SurveyReplyHome;
import com.idega.block.survey.data.SurveyStatus;
import com.idega.block.survey.data.SurveyStatusHome;
import com.idega.block.survey.data.SurveyType;
import com.idega.block.survey.data.SurveyTypeHome;
import com.idega.business.IBOServiceBean;
import com.idega.core.localisation.data.ICLocale;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;

/**
 * Title: SurveyBusinessBean Description: Copyright: Copyright (c) 2004 Company:
 * idega Software
 * 
 * @author 2004 - idega team - <br>
 *         <a href="mailto:gummi@idega.is">Gudmundur Agust Saemundsson</a><br>
 * @version 1.0
 */
public class SurveyBusinessBean extends IBOServiceBean implements
		SurveyBusiness {

	public final static char ANSWERTYPE_SINGLE_CHOICE = 's';
	public final static char ANSWERTYPE_MULTI_CHOICE = 'm';
	public final static char ANSWERTYPE_TEXTAREA = 't';

	/**
	 * 
	 */
	public SurveyBusinessBean() {
		super();
	}

	public SurveyEntity createSurvey(InformationFolder folder, String name,
			String description, IWTimestamp startTime, IWTimestamp endTime,
			String surveyTypePK) throws IDOLookupException, CreateException {
		SurveyEntity survey = getSurveyHome().create();

		survey.setFolder(folder.getEntity());

		survey.setName(name);
		if (description != null) {
			survey.setDescription(description);
		}

		if (startTime != null) {
			survey.setStartTime(startTime.getTimestamp());
		}

		if (endTime != null) {
			survey.setEndTime(endTime.getTimestamp());
		}
		if (surveyTypePK != null) {
			try {
				survey.setSurveyType(getSurveyTypeHome().findByPrimaryKey(
						surveyTypePK));
			} catch (FinderException e) {
				e.printStackTrace();
			}
		}

		survey.store();

		return survey;
	}

	public SurveyQuestion createSurveyQuestion(SurveyEntity survey,
			String[] question, ICLocale[] locale, char answerType)
			throws IDOLookupException, CreateException,
			IDOAddRelationshipException {
		SurveyQuestion sQuestion = getQuestionHome().create();

		if (question != null && locale != null) {
			for (int i = 0; i < question.length && i < locale.length; i++) {
				sQuestion.setQuestion(question[i], locale[i]);
			}
		}

		sQuestion.setAnswerType(answerType);

		sQuestion.store();

		survey.addQuestion(sQuestion);

		return sQuestion;
	}

	public SurveyQuestion createSurveyQuestion(SurveyEntity survey,
			String question, ICLocale locale, char answerType)
			throws IDOLookupException, IDOAddRelationshipException,
			CreateException {
		String[] questions = { question };
		ICLocale[] locales = { locale };
		return createSurveyQuestion(survey, questions, locales, answerType);
	}

	public SurveyAnswer createSurveyAnswer(SurveyQuestion question,
			String[] answer, ICLocale[] locale) throws IDOLookupException,
			CreateException {
		SurveyAnswer sAnswer = getAnswerHome().create();

		if (answer != null && locale != null) {
			for (int i = 0; i < answer.length && i < locale.length; i++) {
				sAnswer.setAnswer((answer[i] == null) ? "" : answer[i],
						locale[i]);
			}
		}

		sAnswer.setSurveyQuestion(question);

		sAnswer.store();

		return sAnswer;
	}

	public SurveyAnswer createSurveyAnswer(SurveyQuestion question,
			String answer, ICLocale locale) throws IDOLookupException,
			CreateException {
		String[] answers = { (answer == null) ? "" : answer };
		ICLocale[] locales = { locale };
		return createSurveyAnswer(question, answers, locales);
	}

	public SurveyParticipant createSurveyParticipant(String participantKey,
			SurveyEntity survey, User user, boolean alwaysCreateNew)
			throws IDOLookupException, CreateException {
		SurveyParticipant participant;
		if (alwaysCreateNew) {
			participant = getSurveyParticipantHome().create();
			participant.setParticipantName(participantKey);
			participant.setSurvey(survey);
			participant.setUser(user);
			participant.setEntryDate(IWTimestamp.getTimestampRightNow());

			participant.store();
		} else {
			try {
				participant = getSurveyParticipantHome().findParticipant(
						survey, user);
			} catch (FinderException e) {
				participant = getSurveyParticipantHome().create();
				participant.setParticipantName(participantKey);
				participant.setSurvey(survey);
				participant.setUser(user);
				participant.setEntryDate(IWTimestamp.getTimestampRightNow());

				participant.store();
			}
		}

		return participant;
	}

	public void removeSurveyRepliesForParticipant(SurveyEntity survey,
			SurveyParticipant participant) {
		try {
			Collection col = getSurveyReplyHome().findBySurveyAndParticipant(
					survey, participant);
			if (col != null && !col.isEmpty()) {
				Iterator it = col.iterator();
				while (it.hasNext()) {
					SurveyReply reply = (SurveyReply) it.next();
					reply.remove();
				}
			}
		} catch (IDOLookupException e) {
			e.printStackTrace();
		} catch (FinderException e) {
		} catch (EJBException e) {
			e.printStackTrace();
		} catch (RemoveException e) {
			e.printStackTrace();
		}
	}

	public SurveyReply createSurveyReply(SurveyEntity survey,
			SurveyQuestion question, SurveyParticipant participant,
			SurveyAnswer answer, String answerText, IWTimestamp entryDate)
			throws IDOLookupException, CreateException {
		SurveyReply reply = getSurveyReplyHome().create();

		reply.setSurvey(survey);
		reply.setQuestion(question);
		reply.setParticipantKey(participant.getParticipantName());
		reply.setParticipant(participant);
		reply.setEntryDate(entryDate.getTimestamp());

		if (answer != null) {
			reply.setAnswer(answer);
		}

		if (answerText != null) {
			if (answerText.length() > SurveyConstants.SURVEY_ANSWER_MAX_LENGTH) {
				reply.setAnswer(answerText.substring(0,
						SurveyConstants.SURVEY_ANSWER_MAX_LENGTH));
				reply.store();
				createSurveyReply(
						survey,
						question,
						participant,
						answer,
						answerText
								.substring(SurveyConstants.SURVEY_ANSWER_MAX_LENGTH),
						entryDate);
			} else {
				reply.setAnswer(answerText);
			}
		}

		reply.store();

		return reply;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.idega.block.survey.business.SurveyBusiness#updateSurveyQuestion(com.idega.block.survey.data.SurveyEntity,
	 *      com.idega.block.survey.data.SurveyQuestion, java.lang.String,
	 *      com.idega.core.localisation.data.ICLocale, char)
	 */
	public SurveyQuestion updateSurveyQuestion(SurveyEntity survey,
			SurveyQuestion question, String questionText, ICLocale locale,
			char type) throws IDOLookupException, CreateException {
		// ??use surveyEntity to see if the question is related to more than
		// this one and then create new Question
		question.setQuestion(questionText, locale);
		question.setAnswerType(type);
		question.store();
		return question;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.idega.block.survey.business.SurveyBusiness#updateSurveyAnswer(com.idega.block.survey.data.SurveyAnswer,
	 *      java.lang.String, com.idega.core.localisation.data.ICLocale)
	 */
	public SurveyAnswer updateSurveyAnswer(SurveyAnswer ans,
			String answerString, ICLocale locale) throws IDOLookupException,
			CreateException {
		ans.setAnswer(answerString, locale);
		ans.store();
		return ans;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.idega.block.survey.business.SurveyBusiness#removeQuestionFromSurvey(com.idega.block.survey.data.SurveyEntity,
	 *      com.idega.block.survey.data.SurveyQuestion)
	 */
	public void removeQuestionFromSurvey(SurveyEntity survey,
			SurveyQuestion question, User user)
			throws IDORemoveRelationshipException {
		// Collection answers =
		// this.getAnswerHome().findQuestionsAnswer(question);
		// for (Iterator aIter = answers.iterator(); aIter.hasNext();) {
		// this.removeAnswerFromQuestion(question,(SurveyAnswer)aIter.next(),user);
		// }
		survey.removeQuestion(question);
		question.setRemoved(user);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.idega.block.survey.business.SurveyBusiness#removeAnswerFromQuestion(com.idega.block.survey.data.SurveyQuestion,
	 *      com.idega.block.survey.data.SurveyAnswer)
	 */
	public void removeAnswer(SurveyAnswer ans, User user)
			throws IDORemoveRelationshipException {
		ans.setRemoved(user);
	}

	/**
	 * @return
	 */
	public SurveyAnswerHome getAnswerHome() throws IDOLookupException {
		return (SurveyAnswerHome) IDOLookup.getHome(SurveyAnswer.class);
	}

	/**
	 * @return
	 */
	public SurveyEntityHome getSurveyHome() throws IDOLookupException {
		return (SurveyEntityHome) IDOLookup.getHome(SurveyEntity.class);
	}

	public SurveyTypeHome getSurveyTypeHome() throws IDOLookupException {
		return (SurveyTypeHome) IDOLookup.getHome(SurveyType.class);
	}

	/**
	 * @return
	 */
	public SurveyQuestionHome getQuestionHome() throws IDOLookupException {
		return (SurveyQuestionHome) IDOLookup.getHome(SurveyQuestion.class);
	}

	/**
	 * @return
	 */
	public SurveyReplyHome getSurveyReplyHome() throws IDOLookupException {
		return (SurveyReplyHome) IDOLookup.getHome(SurveyReply.class);
	}

	/**
	 * @return
	 */
	public SurveyParticipantHome getSurveyParticipantHome()
			throws IDOLookupException {
		return (SurveyParticipantHome) IDOLookup
				.getHome(SurveyParticipant.class);
	}

	public SurveyStatusHome getSurveyStatusHome() throws IDOLookupException {
		return (SurveyStatusHome) IDOLookup.getHome(SurveyStatus.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.idega.block.survey.business.SurveyBusiness#reportParticipation(com.idega.block.survey.data.SurveyEntity,
	 *      java.lang.String)
	 */
	public SurveyParticipant reportParticipation(SurveyEntity survey,
			String participant) throws IDOLookupException, CreateException {
		SurveyParticipant sp = getSurveyParticipantHome().create();
		sp.setSurvey(survey);
		sp.setParticipantName(participant);
		sp.store();
		return sp;
	}

	public SurveyStatus getSurveyStatus(SurveyEntity survey) {
		try {
			try {
				return getSurveyStatusHome().findBySurvey(survey);
			} catch (IDOLookupException e) {
				e.printStackTrace();
			}
		} catch (FinderException e) {
			try {
				SurveyStatus status = getSurveyStatusHome().create();
				status.setSurvey(survey);
				status.setIsModified(true);
				status.store();
				return status;
			} catch (CreateException e1) {
				e1.printStackTrace();
			} catch (IDOLookupException e2) {
				e2.printStackTrace();
			}
		}

		return null;
	}

	public static final String SURVEY_TYPE_QUESTIONNAIRE = "questionnaire";
	public static final String SURVEY_TYPE_EXAM = "exam";
	public static final String SURVEY_TYPE_TEST = "test";
	private static final String DEFAULT_SURVEY_TYPE = SURVEY_TYPE_QUESTIONNAIRE;

	public SurveyType getSurveyType(SurveyEntity survey) throws FinderException {
		if (survey != null) {
			SurveyType type = survey.getSurveyType();
			if (type == null) {
				try {
					SurveyTypeHome stHome = (SurveyTypeHome) IDOLookup
							.getHome(SurveyType.class);
					type = stHome.findByName(DEFAULT_SURVEY_TYPE);
					survey.setSurveyType(type);
					survey.store();
					log("Setting default Survey Type (" + DEFAULT_SURVEY_TYPE
							+ ") to the Survey " + survey.getName());
				} catch (IDOLookupException e) {
					logError("Could not find default survey type : "
							+ DEFAULT_SURVEY_TYPE);
				}
			}
			return type;
		}
		return null;
	}

	public DropdownMenu getSurveyTypeDropdownMenu(IWResourceBundle iwrb,
			String name) {
		DropdownMenu menu = new DropdownMenu(name);
		Collection types = getSurveyTypes();
		if (types != null) {
			Iterator iter = types.iterator();
			while (iter.hasNext()) {
				SurveyType t = (SurveyType) iter.next();
				menu.addMenuElement(t.getPrimaryKey().toString(),
						iwrb.getLocalizedString(t.getLocalizationKey(), t
								.getName()));
			}
		}
		return menu;
	}

	public Collection getSurveyTypes() {
		try {
			SurveyTypeHome stHome = (SurveyTypeHome) IDOLookup
					.getHome(SurveyType.class);
			return stHome.findAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
