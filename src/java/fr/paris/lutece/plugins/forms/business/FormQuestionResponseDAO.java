/*
 * Copyright (c) 2002-2018, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */

package fr.paris.lutece.plugins.forms.business;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import fr.paris.lutece.plugins.genericattributes.business.FieldHome;
import fr.paris.lutece.plugins.genericattributes.business.Response;
import fr.paris.lutece.plugins.genericattributes.business.ResponseFilter;
import fr.paris.lutece.plugins.genericattributes.business.ResponseHome;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * This class provides Data Access methods for Form objects
 */
public final class FormQuestionResponseDAO implements IFormQuestionResponseDAO
{
    // Constants
    private static final String SQL_QUERY_SELECTALL = "SELECT id_question_response, id_form_response, id_question, id_step, iteration_number FROM forms_question_response";
    private static final String SQL_QUERY_SELECT = SQL_QUERY_SELECTALL + " WHERE id_question_response = ?";
    private static final String SQL_QUERY_SELECT_BY_FORM_RESPONSE = SQL_QUERY_SELECTALL + " WHERE id_form_response = ?";
    private static final String SQL_QUERY_INSERT = "INSERT INTO forms_question_response ( id_form_response, id_question, id_step, iteration_number ) VALUES ( ?, ?, ?, ? ) ";
    private static final String SQL_QUERY_DELETE = "DELETE FROM forms_question_response WHERE id_question_response = ? ";
    private static final String SQL_QUERY_UPDATE = "UPDATE forms_question_response SET id_form_response = ?, id_question = ?, id_step = ?, iteration_number = ? WHERE id_question_response = ?";
    private static final String SQL_QUERY_SELECT_BY_QUESTION = SQL_QUERY_SELECTALL + " WHERE id_question = ?";
    private static final String SQL_QUERY_SELECT_BY_RESPONSE_AND_QUESTION = SQL_QUERY_SELECTALL + " WHERE id_form_response = ? AND id_question = ?";
    private static final String SQL_QUERY_SELECT_BY_RESPONSE_AND_STEP = SQL_QUERY_SELECTALL
            + " WHERE id_form_response = ? AND id_step = ? ORDER BY id_question_response ASC";
    private static final String SQL_QUERY_SELECT_BY_LIST_RESPONSE_STEP = SQL_QUERY_SELECTALL + " WHERE ";

    private static final FormQuestionEntryResponseDAO _formQuestionEntryResponseDAO = new FormQuestionEntryResponseDAO( );

    /**
     * {@inheritDoc }
     */
    @Override
    public void insert( FormQuestionResponse formQuestionResponse, Plugin plugin )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin );

        int nIndex = 1;
        daoUtil.setInt( nIndex++, formQuestionResponse.getIdFormResponse( ) );
        daoUtil.setInt( nIndex++, formQuestionResponse.getQuestion( ).getId( ) );
        daoUtil.setInt( nIndex++, formQuestionResponse.getIdStep( ) );
        daoUtil.setInt( nIndex++, formQuestionResponse.getQuestion( ).getIterationNumber( ) );

        daoUtil.executeUpdate( );

        if ( daoUtil.nextGeneratedKey( ) )
        {
            formQuestionResponse.setId( daoUtil.getGeneratedKeyInt( 1 ) );
        }

        daoUtil.close( );

        for ( Response response : formQuestionResponse.getEntryResponse( ) )
        {
            FormQuestionEntryResponse formQuestionEntryResponse = new FormQuestionEntryResponse( );
            formQuestionEntryResponse._nIdQuestionResponse = formQuestionResponse.getId( );
            formQuestionEntryResponse._response = response;

            _formQuestionEntryResponseDAO.insert( formQuestionEntryResponse, plugin );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FormQuestionResponse load( int nKey, Plugin plugin )
    {
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin );
        daoUtil.setInt( 1, nKey );
        daoUtil.executeQuery( );

        FormQuestionResponse formQuestionResponse = null;

        if ( daoUtil.next( ) )
        {
            formQuestionResponse = dataToObject( daoUtil );
        }

        daoUtil.close( );

        completeWithEntryResponses( formQuestionResponse, plugin );

        return formQuestionResponse;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void delete( FormQuestionResponse formQuestionResponse, Plugin plugin )
    {
        List<FormQuestionEntryResponse> listFormQuestionEntryResponse = _formQuestionEntryResponseDAO.selectByFormQuestionResponse( formQuestionResponse,
                plugin );

        for ( FormQuestionEntryResponse formQuestionEntryResponse : listFormQuestionEntryResponse )
        {
            _formQuestionEntryResponseDAO.delete( formQuestionEntryResponse, plugin );
        }

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin );
        daoUtil.setInt( 1, formQuestionResponse.getId( ) );
        daoUtil.executeUpdate( );
        daoUtil.close( );

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FormQuestionResponse> selectFormQuestionResponseByQuestion( int nIdQuestion, Plugin plugin )
    {
        List<FormQuestionResponse> formQuestionResponseList = new ArrayList<>( );

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_QUESTION, plugin );
        daoUtil.setInt( 1, nIdQuestion );
        daoUtil.executeQuery( );

        while ( daoUtil.next( ) )
        {
            formQuestionResponseList.add( dataToObject( daoUtil ) );
        }

        daoUtil.close( );

        completeWithEntryResponses( formQuestionResponseList, plugin );

        return formQuestionResponseList;
    }

    @Override
    public FormQuestionResponse selectFormQuestionResponseByEntryResponse( Response response, Plugin plugin )
    {
        FormQuestionResponse res = null;
        FormQuestionEntryResponse questionEntryResponse = _formQuestionEntryResponseDAO.selectByFormEntryResponse( response, plugin );

        if ( questionEntryResponse != null )
        {
            res = load( questionEntryResponse._nIdQuestionResponse, plugin );
        }
        return res;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void store( FormQuestionResponse formQuestionResponse, Plugin plugin )
    {
        FormQuestionResponse formQuestionResponseSaved = load( formQuestionResponse.getId( ), plugin );

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, Statement.RETURN_GENERATED_KEYS, plugin );

        int nIndex = 1;
        daoUtil.setInt( nIndex++, formQuestionResponse.getIdFormResponse( ) );
        daoUtil.setInt( nIndex++, formQuestionResponse.getQuestion( ).getId( ) );
        daoUtil.setInt( nIndex++, formQuestionResponse.getIdStep( ) );
        daoUtil.setInt( nIndex++, formQuestionResponse.getQuestion( ).getIterationNumber( ) );

        daoUtil.setInt( nIndex++, formQuestionResponse.getId( ) );

        daoUtil.executeUpdate( );

        daoUtil.close( );

        storeResponses( formQuestionResponseSaved, formQuestionResponse, plugin );

    }

    /**
     * Stores the responses associated to the form question response
     * 
     * @param formQuestionResponseSaved
     *            the former form question response (the one saved on database)
     * @param formQuestionResponseNew
     *            the new form question response
     * @param plugin
     *            the plugin
     */
    private void storeResponses( FormQuestionResponse formQuestionResponseSaved, FormQuestionResponse formQuestionResponseNew, Plugin plugin )
    {
        List<FormQuestionEntryResponse> listFormQuestionEntryResponseSaved = _formQuestionEntryResponseDAO.selectByFormQuestionResponse(
                formQuestionResponseSaved, plugin );
        List<Response> listNewResponse = formQuestionResponseNew.getEntryResponse( );

        for ( FormQuestionEntryResponse formQuestionEntryResponseSaved : listFormQuestionEntryResponseSaved )
        {
            _formQuestionEntryResponseDAO.delete( formQuestionEntryResponseSaved, plugin );
        }

        for ( Response responseNew : listNewResponse )
        {
            FormQuestionEntryResponse formQuestionEntryResponse = new FormQuestionEntryResponse( );
            formQuestionEntryResponse._nIdQuestionResponse = formQuestionResponseSaved.getId( );
            formQuestionEntryResponse._response = responseNew;

            _formQuestionEntryResponseDAO.insert( formQuestionEntryResponse, plugin );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<FormQuestionResponse> selectFormQuestionResponseList( Plugin plugin )
    {
        List<FormQuestionResponse> formQuestionResponseList = new ArrayList<FormQuestionResponse>( );
        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin );
        daoUtil.executeQuery( );
        while ( daoUtil.next( ) )
        {
            formQuestionResponseList.add( dataToObject( daoUtil ) );
        }

        daoUtil.close( );

        completeWithEntryResponses( formQuestionResponseList, plugin );

        return formQuestionResponseList;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<FormQuestionResponse> selectFormQuestionResponseListByStepAndFormResponse( int nIdFormResponse, int nIdStep, Plugin plugin )
    {
        List<FormQuestionResponse> formQuestionResponseList = new ArrayList<FormQuestionResponse>( );

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_RESPONSE_AND_STEP, plugin );
        daoUtil.setInt( 1, nIdFormResponse );
        daoUtil.setInt( 2, nIdStep );
        daoUtil.executeQuery( );

        while ( daoUtil.next( ) )
        {
            formQuestionResponseList.add( dataToObject( daoUtil ) );
        }

        daoUtil.close( );

        completeWithEntryResponses( formQuestionResponseList, plugin );

        return formQuestionResponseList;
    }

    @Override
    public List<FormQuestionResponse> selectFormQuestionResponseListByListFormResponseStep( List<FormResponseStep> listFormResponseStep, Plugin plugin )
    {
        String query = SQL_QUERY_SELECT_BY_LIST_RESPONSE_STEP
                + listFormResponseStep.stream( ).map( frs -> " (  id_form_response = ? AND id_step = ? ) " ).collect( Collectors.joining( " OR " ) );

        List<FormQuestionResponse> list = new ArrayList<>( );
        try( DAOUtil daoUtil = new DAOUtil( query, plugin ) )
        {
            int nIndex = 1;
            for ( FormResponseStep formResponseStep : listFormResponseStep )
            {
                daoUtil.setInt( nIndex++, formResponseStep.getFormResponseId( ) );
                daoUtil.setInt( nIndex++, formResponseStep.getStep( ).getId( ) );
            }
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                list.add( dataToObject( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<FormQuestionResponse> selectFormQuestionResponseListByFormResponse( int nIdFormResponse, Plugin plugin )
    {
        List<FormQuestionResponse> formQuestionResponseList = new ArrayList<FormQuestionResponse>( );

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_FORM_RESPONSE, plugin );
        daoUtil.setInt( 1, nIdFormResponse );
        daoUtil.executeQuery( );

        while ( daoUtil.next( ) )
        {
            formQuestionResponseList.add( dataToObject( daoUtil ) );
        }

        daoUtil.close( );

        completeWithEntryResponses( formQuestionResponseList, plugin );

        return formQuestionResponseList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FormQuestionResponse> selectFormQuestionResponseByResponseForQuestion( int nIdFormResponse, int nIdQuestion, Plugin plugin )
    {
        List<FormQuestionResponse> listFormQuestionResponse = new ArrayList<>( );

        DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_RESPONSE_AND_QUESTION, plugin );

        try
        {
            daoUtil.setInt( 1, nIdFormResponse );
            daoUtil.setInt( 2, nIdQuestion );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                FormQuestionResponse formQuestionResponse = dataToObject( daoUtil );
                completeWithEntryResponses( formQuestionResponse, plugin );

                listFormQuestionResponse.add( formQuestionResponse );
            }
        }
        finally
        {
            daoUtil.close( );
        }

        return listFormQuestionResponse;
    }

    /**
     * Completes the specified form question response with the entry responses
     * 
     * @param formQuestionResponse
     *            The form question response
     * @param plugin
     *            The plugin to use to execute the query
     */
    private void completeWithEntryResponses( FormQuestionResponse formQuestionResponse, Plugin plugin )
    {
        if ( formQuestionResponse != null )
        {
            List<FormQuestionEntryResponse> listFormQuestionEntryResponse = _formQuestionEntryResponseDAO.selectByFormQuestionResponse( formQuestionResponse,
                    plugin );

            List<Response> listEntryResponse = new ArrayList<>( );

            for ( FormQuestionEntryResponse formQuestionEntryResponse : listFormQuestionEntryResponse )
            {
                Response response = formQuestionEntryResponse._response;

                if ( response != null )
                {
                    listEntryResponse.add( response );

                    if ( response.getField( ) != null )
                    {
                        response.setField( FieldHome.findByPrimaryKey( response.getField( ).getIdField( ) ) );
                    }
                }
            }

            formQuestionResponse.setEntryResponse( listEntryResponse );
        }
    }

    @Override
    public void completeListWithEntryResponses( List<FormQuestionResponse> formQuestionResponsesList, Plugin plugin )
    {
        if ( CollectionUtils.isNotEmpty( formQuestionResponsesList ) )
        {
            List<FormQuestionEntryResponse> listFormQuestionEntryResponse = _formQuestionEntryResponseDAO.selectByFormQuestionResponseList(
                    formQuestionResponsesList, plugin );

            for ( FormQuestionResponse formQuestionResponse : formQuestionResponsesList )
            {
                List<FormQuestionEntryResponse> entryResponseList = listFormQuestionEntryResponse.stream( )
                        .filter( entry -> entry._nIdQuestionResponse == formQuestionResponse.getId( ) ).collect( Collectors.toList( ) );
                formQuestionResponse.setEntryResponse( entryResponseList.stream( ).map( entryResponse -> entryResponse._response ).filter( Objects::nonNull )
                        .collect( Collectors.toList( ) ) );
            }
        }
    }

    /**
     * Completes the specified list of form question responses with the entry responses
     * 
     * @param formQuestionResponseList
     *            The list of form question responses
     * @param plugin
     *            The plugin to use to execute the query
     */
    private void completeWithEntryResponses( List<FormQuestionResponse> formQuestionResponseList, Plugin plugin )
    {
        if ( !CollectionUtils.isEmpty( formQuestionResponseList ) )
        {
            for ( FormQuestionResponse formQuestionResponse : formQuestionResponseList )
            {
                completeWithEntryResponses( formQuestionResponse, plugin );
            }
        }
    }

    /**
     * Creates a form question response from the specified {@code DAOUtil} object
     * 
     * @param daoUtil
     *            The {@code DAOUtil} object
     * @return The created form question response
     */
    private FormQuestionResponse dataToObject( DAOUtil daoUtil )
    {
        FormQuestionResponse formQuestionResponse = new FormQuestionResponse( );

        formQuestionResponse.setId( daoUtil.getInt( "id_question_response" ) );
        formQuestionResponse.setIdFormResponse( daoUtil.getInt( "id_form_response" ) );

        Question question = new Question( );
        question.setId( daoUtil.getInt( "id_question" ) );
        formQuestionResponse.setQuestion( question );

        formQuestionResponse.setIdStep( daoUtil.getInt( "id_step" ) );
        question.setIterationNumber( daoUtil.getInt( "iteration_number" ) );

        return formQuestionResponse;
    }

    /**
     * <p>
     * This class represents a form question entry response.
     * </p>
     * <p>
     * It associates a form question response with a entry response
     * </p>
     *
     */
    private static class FormQuestionEntryResponse
    {
        private int _nId;
        private int _nIdQuestionResponse;
        private Response _response;
    }

    /**
     * This class provides CRUD methods for a {@code FormQuestionEntryResponse} object
     *
     */
    private static class FormQuestionEntryResponseDAO
    {
        private static final String SQL_QUERY_SELECT_ALL = "SELECT id_question_entry_response, id_question_response, id_entry_response FROM forms_question_entry_response";
        private static final String SQL_QUERY_SELECT_ENTRY_RESPONSE_BY_QUESTION = SQL_QUERY_SELECT_ALL + " WHERE id_question_response = ?";
        private static final String SQL_QUERY_SELECT_ENTRY_RESPONSE_BY_RESPONSE = SQL_QUERY_SELECT_ALL + " WHERE id_entry_response = ?";
        private static final String SQL_QUERY_INSERT_ENTRY_RESPONSE = "INSERT INTO forms_question_entry_response ( id_question_response, id_entry_response ) VALUES ( ?, ? ) ";
        private static final String SQL_QUERY_DELETE_QUESTION_ENTRY_RESPONSE = "DELETE FROM forms_question_entry_response WHERE id_question_entry_response = ?";
        private static final String SQL_QUERY_SELECT_IN = SQL_QUERY_SELECT_ALL + " WHERE id_question_response IN ( ";

        /**
         * Selects the form question entry responses for the specified form question response
         * 
         * @param formQuestionResponse
         *            the form question response
         * @param plugin
         *            the plugin
         * @return the list of form question entry responses
         */
        private List<FormQuestionEntryResponse> selectByFormQuestionResponse( FormQuestionResponse formQuestionResponse, Plugin plugin )
        {
            DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ENTRY_RESPONSE_BY_QUESTION, plugin );
            List<FormQuestionEntryResponse> listFormQuestionEntryResponse = new ArrayList<>( );

            try
            {
                daoUtil.setInt( 1, formQuestionResponse.getId( ) );
                daoUtil.executeQuery( );

                while ( daoUtil.next( ) )
                {
                    listFormQuestionEntryResponse.add( dataToObject( daoUtil ) );
                }
            }
            finally
            {
                daoUtil.close( );
            }

            return listFormQuestionEntryResponse;

        }

        /**
         * Selects the form question entry responses for the specified form question response
         * 
         * @param formQuestionResponseList
         *            the form question response list
         * @param plugin
         *            the plugin
         * @return the list of form question entry responses
         */
        private List<FormQuestionEntryResponse> selectByFormQuestionResponseList( List<FormQuestionResponse> formQuestionResponseList, Plugin plugin )
        {
            List<Integer> idList = formQuestionResponseList.stream( ).map( FormQuestionResponse::getId ).distinct( ).collect( Collectors.toList( ) );
            String query = SQL_QUERY_SELECT_IN + idList.stream( ).distinct( ).map( i -> "?" ).collect( Collectors.joining( "," ) ) + " )";

            List<FormQuestionEntryResponse> listFormQuestionEntryResponse = new ArrayList<>( );

            try( DAOUtil daoUtil = new DAOUtil( query, plugin ) )
            {
                for ( int i = 0; i < idList.size( ); i++ )
                {
                    daoUtil.setInt( i + 1, idList.get( i ) );
                }
                daoUtil.executeQuery( );

                while ( daoUtil.next( ) )
                {
                    listFormQuestionEntryResponse.add( dataToObjectWithoutResponse( daoUtil ) );
                }
            }
            ResponseFilter filter = new ResponseFilter( );
            filter.setListId( listFormQuestionEntryResponse.stream( ).map( entryReponse -> entryReponse._response.getIdResponse( ) ).distinct( )
                    .collect( Collectors.toList( ) ) );

            List<Response> responseList = ResponseHome.getResponseList( filter );

            for ( FormQuestionEntryResponse formQuestionEntryResponse : listFormQuestionEntryResponse )
            {
                Integer responseId = formQuestionEntryResponse._response.getIdResponse( );
                formQuestionEntryResponse._response = responseList.stream( ).filter( resp -> resp.getIdResponse( ) == responseId ).findFirst( ).orElse( null );
            }

            return listFormQuestionEntryResponse;

        }

        /**
         * Selects the form question entry responses for the specified entry response
         * 
         * @param formQuestionResponse
         *            the form question response
         * @param plugin
         *            the plugin
         * @return the list of form question entry responses
         */
        private FormQuestionEntryResponse selectByFormEntryResponse( Response response, Plugin plugin )
        {
            FormQuestionEntryResponse res = null;
            try( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ENTRY_RESPONSE_BY_RESPONSE, plugin ) )
            {
                daoUtil.setInt( 1, response.getIdResponse( ) );
                daoUtil.executeQuery( );

                if ( daoUtil.next( ) )
                {
                    res = dataToObject( daoUtil );
                }
            }

            return res;
        }

        /**
         * Inserts the specified form question entry response
         * 
         * @param formQuestionEntryResponse
         *            the form question entry response to insert
         * @param plugin
         *            the plugin
         */
        private void insert( FormQuestionEntryResponse formQuestionEntryResponse, Plugin plugin )
        {
            ResponseHome.create( formQuestionEntryResponse._response );

            DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT_ENTRY_RESPONSE, Statement.RETURN_GENERATED_KEYS, plugin );

            int nIndex = 1;
            daoUtil.setInt( nIndex++, formQuestionEntryResponse._nIdQuestionResponse );
            daoUtil.setInt( nIndex++, formQuestionEntryResponse._response.getIdResponse( ) );

            daoUtil.executeUpdate( );

            daoUtil.close( );
        }

        /**
         * Deletes the specified form question entry response
         * 
         * @param formQuestionEntryResponse
         *            the orm question entry response
         * @param plugin
         *            the plugin
         */
        private void delete( FormQuestionEntryResponse formQuestionEntryResponse, Plugin plugin )
        {
            DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_QUESTION_ENTRY_RESPONSE, plugin );

            daoUtil.setInt( 1, formQuestionEntryResponse._nId );
            daoUtil.executeUpdate( );

            daoUtil.close( );

            if ( formQuestionEntryResponse._response != null )
            {
                ResponseHome.remove( formQuestionEntryResponse._response.getIdResponse( ) );
            }
        }

        /**
         * Creates a form question entry response from the specified {@code DAOUtil} object
         * 
         * @param daoUtil
         *            The {@code DAOUtil} object
         * @return The created form question entry response
         */
        private FormQuestionEntryResponse dataToObject( DAOUtil daoUtil )
        {
            FormQuestionEntryResponse formQuestionEntryResponse = new FormQuestionEntryResponse( );
            formQuestionEntryResponse._nId = daoUtil.getInt( "id_question_entry_response" );
            formQuestionEntryResponse._nIdQuestionResponse = daoUtil.getInt( "id_question_response" );
            formQuestionEntryResponse._response = ResponseHome.findByPrimaryKey( daoUtil.getInt( "id_entry_response" ) );

            return formQuestionEntryResponse;
        }

        /**
         * Creates a form question entry response from the specified {@code DAOUtil} object
         * 
         * @param daoUtil
         *            The {@code DAOUtil} object
         * @return The created form question entry response
         */
        private FormQuestionEntryResponse dataToObjectWithoutResponse( DAOUtil daoUtil )
        {
            FormQuestionEntryResponse formQuestionEntryResponse = new FormQuestionEntryResponse( );
            formQuestionEntryResponse._nId = daoUtil.getInt( "id_question_entry_response" );
            formQuestionEntryResponse._nIdQuestionResponse = daoUtil.getInt( "id_question_response" );
            formQuestionEntryResponse._response = new Response( );
            formQuestionEntryResponse._response.setIdResponse( daoUtil.getInt( "id_entry_response" ) );

            return formQuestionEntryResponse;
        }
    }

}
