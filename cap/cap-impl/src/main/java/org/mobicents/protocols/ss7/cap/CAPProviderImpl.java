/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.cap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.mobicents.protocols.asn.AsnException;
import org.mobicents.protocols.asn.AsnInputStream;
import org.mobicents.protocols.asn.AsnOutputStream;
import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPDialogListener;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPParsingComponentException;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.CAPServiceBase;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPComponentErrorReason;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGeneralAbortReason;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGprsReferenceNumber;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPNoticeProblemDiagnostic;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorCode;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessageFactory;
import org.mobicents.protocols.ss7.cap.dialog.CAPGprsReferenceNumberImpl;
import org.mobicents.protocols.ss7.cap.dialog.CAPUserAbortPrimitiveImpl;
import org.mobicents.protocols.ss7.cap.errors.CAPErrorMessageFactoryImpl;
import org.mobicents.protocols.ss7.cap.errors.CAPErrorMessageImpl;
import org.mobicents.protocols.ss7.map.MAPDialogImpl;
import org.mobicents.protocols.ss7.map.MAPDialogState;
import org.mobicents.protocols.ss7.map.MAPServiceBaseImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPDialogueAS;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParsingComponentException;
import org.mobicents.protocols.ss7.map.api.MAPServiceBase;
import org.mobicents.protocols.ss7.map.api.dialog.MAPProviderAbortReason;
import org.mobicents.protocols.ss7.map.api.dialog.ServingCheckData;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.MAPExtensionContainer;
import org.mobicents.protocols.ss7.map.dialog.MAPOpenInfoImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPSendException;
import org.mobicents.protocols.ss7.tcap.api.TCListener;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCBeginIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCBeginRequest;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCContinueIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCContinueRequest;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCEndIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCEndRequest;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCPAbortIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUniIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUserAbortIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUserAbortRequest;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TerminationType;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.DialogServiceUserType;
import org.mobicents.protocols.ss7.tcap.asn.InvokeImpl;
import org.mobicents.protocols.ss7.tcap.asn.TcapFactory;
import org.mobicents.protocols.ss7.tcap.asn.UserInformation;
import org.mobicents.protocols.ss7.tcap.asn.comp.Component;
import org.mobicents.protocols.ss7.tcap.asn.comp.ComponentType;
import org.mobicents.protocols.ss7.tcap.asn.comp.ErrorCodeType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.protocols.ss7.tcap.asn.comp.InvokeProblemType;
import org.mobicents.protocols.ss7.tcap.asn.comp.OperationCode;
import org.mobicents.protocols.ss7.tcap.asn.comp.OperationCodeType;
import org.mobicents.protocols.ss7.tcap.asn.comp.PAbortCauseType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Parameter;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.protocols.ss7.tcap.asn.comp.ProblemType;
import org.mobicents.protocols.ss7.tcap.asn.comp.Reject;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnError;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnErrorProblemType;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResultLast;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResultProblemType;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CAPProviderImpl implements CAPProvider, TCListener {

	protected Logger loger = Logger.getLogger(CAPProviderImpl.class);

	private List<CAPDialogListener> dialogListeners = new CopyOnWriteArrayList<CAPDialogListener>();

	protected Map<Long, CAPDialogImpl> dialogs = new HashMap<Long, CAPDialogImpl>();

	private TCAPProvider tcapProvider = null;

	private final CAPParameterFactory capParameterFactory = new CAPParameterFactoryImpl();
	private final CAPErrorMessageFactory capErrorMessageFactory = new CAPErrorMessageFactoryImpl();

	protected Set<CAPServiceBase> capServices = new HashSet<CAPServiceBase>();
//	private final CAPServiceSms capServiceSms = new CAPServiceSmsImpl(this);
	

	public CAPProviderImpl(TCAPProvider tcapProvider) {
		this.tcapProvider = tcapProvider;

//		this.capServices.add(this.capServiceSms);
	}

	public TCAPProvider getTCAPProvider() {
		return this.tcapProvider;
	}

//	public CAPServiceSms getCAPServiceSms() {
//		return this.capServiceSms;
//	}


	@Override
	public void addCAPDialogListener(CAPDialogListener capDialogListener) {
		this.dialogListeners.add(capDialogListener);
	}

	@Override
	public CAPParameterFactory getCAPParameterFactory() {
		return capParameterFactory;
	}
	
	@Override
	public CAPErrorMessageFactory getCAPErrorMessageFactory() {
		return this.capErrorMessageFactory;
	}


	@Override
	public void removeCAPDialogListener(CAPDialogListener capDialogListener) {
		this.dialogListeners.remove(capDialogListener);
	}

	@Override
	public CAPDialog getCAPDialog(Long dialogId) {
		synchronized (this.dialogs) {
			return this.dialogs.get(dialogId);
		}
	}

	public void start() {
		this.tcapProvider.addTCListener(this);
	}

	public void stop() {
		this.tcapProvider.removeTCListener(this);

	}

	protected void addDialog(CAPDialogImpl dialog) {
		synchronized (this.dialogs) {
			this.dialogs.put(dialog.getDialogId(), dialog);
		}
	}

	protected void removeDialog(Long dialogId) {
		synchronized (this.dialogs) {
			this.dialogs.remove(dialogId);
		}
	}
	
	public void onTCBegin(TCBeginIndication tcBeginIndication) {
		
		ApplicationContextName acn = tcBeginIndication.getApplicationContextName();
		Component[] comps = tcBeginIndication.getComponents();

		// TODO ....................... CAPGeneralAbortReason CAPUserAbortReason
//		// ACN must be present in CAMEL
//		if (acn == null) {
//			loger.warn("Received TCBeginIndication without application context name");
//
//			try {
//				this.fireTCAbort(tcBeginIndication.getDialog(), CAPGeneralAbortReason.BadReasonData, CAPUserAbortReason);
//			} catch (MAPException e) {
//				loger.error("Error while firing TC-U-ABORT. ", e);
//			}
//			return;
//		}
//
//		MAPApplicationContext mapAppCtx = null;
//		MAPServiceBase perfSer = null;
//		if (acn == null) {
//			// ApplicationContext is absent but components are absent - MAP
//			// Version 1
//
//			// - if no application-context-name is included in the primitive and
//			// if presence of components is indicated, wait for the first
//			// TC-INVOKE primitive, and derive a version 1
//			// application-context-name from the operation code according to
//			// table 12.1/1 (note 1);
//
//			// a) if no application-context-name can be derived (i.e. the
//			// operation code does not exist in MAP V1 specifications), the MAP
//			// PM shall issue a TC-U-ABORT request primitive (note 2). The local
//			// MAP-User is not informed.
//
//			// Extracting Invoke and operationCode
//			Invoke invoke = null;
//			int operationCode = -1;
//			for (Component c : comps) {
//				if (c.getType() == ComponentType.Invoke) {
//					invoke = (Invoke) c;
//					break;
//				}
//			}
//			if (invoke != null) {
//				OperationCode oc = invoke.getOperationCode();
//				if (oc != null && oc.getOperationType() == OperationCodeType.Local) {
//					operationCode = (int) (long) oc.getLocalOperationCode();
//				}
//			}
//			if (operationCode != -1) {
//				// Selecting the MAP service that can perform the operation, getting
//				// ApplicationContext
//				for (MAPServiceBase ser : this.mapServices) {
//					MAPApplicationContext ac = ((MAPServiceBaseImpl)ser).getMAPv1ApplicationContext(operationCode, invoke);
//					if (ac != null) {
//						perfSer = ser;
//						mapAppCtx = ac;
//						break;
//					}
//				}
//			}
//			
//			if (mapAppCtx == null) {
//				// Invoke not found or has bad operationCode or operationCode is not supported
//				try {
//					this.fireTCAbortV1(tcBeginIndication.getDialog());
//					return;
//				} catch (MAPException e) {
//					loger.error("Error while firing TC-U-ABORT. ", e);
//				}
//			}
//		} else {
//			// ApplicationContext is present - MAP Version 2 or higher
//			if (MAPApplicationContext.getProtocolVersion(acn.getOid()) < 2) {
//				// if a version 1 application-context-name is included, the MAP
//				// PM shall issue a TC-U-ABORT
//				// request primitive with abort-reason "User-specific" and
//				// user-information "MAP-ProviderAbortInfo"
//				// indicating "abnormalDialogue". The local MAP-user shall not
//				// be informed.
//				loger.error("Bad version of ApplicationContext if ApplicationContext exists. Must be 2 or greater");
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e) {
//					loger.error("Error while firing TC-U-ABORT. ", e);
//				}
//				return;
//			}
//			
//			mapAppCtx = MAPApplicationContext.getInstance(acn.getOid());
//			
//			// Check if ApplicationContext is recognizable for the implemented
//			// services
//			// If no - TC-U-ABORT - ACN-Not-Supported
//
//			if (mapAppCtx == null) {
//				StringBuffer s = new StringBuffer();
//				s.append("Unrecognizable ApplicationContextName is received: ");
//				for (long l : acn.getOid()) {
//					s.append(l).append(", ");
//				}
//
//				loger.error(s.toString());
//				try {
//					this.fireTCAbortACNNotSupported(tcBeginIndication.getDialog(), null, null);
//				} catch (MAPException e) {
//					loger.error("Error while firing TC-U-ABORT. ", e);
//				}
//
//				return;
//			}
//		}
//
//		AddressString destReference = null;
//		AddressString origReference = null;
//		MAPExtensionContainer extensionContainer = null;
//
//		UserInformation userInfo = tcBeginIndication.getUserInformation();
//		if (userInfo == null) {
//			// if no User-information is present it is checked whether
//			// presence of User Information in the
//			// TC-BEGIN indication primitive is required for the received
//			// application-context-name. If User
//			// Information is required but not present, a TC-U-ABORT request
//			// primitive with abort-reason
//			// "User-specific" and user-information "MAP-ProviderAbortInfo"
//			// indicating "abnormalDialogue"
//			// shall be issued. The local MAP-user shall not be informed.
//
//			// TODO : From where do we know id userInfo is required for a
//			// give
//			// application-context-name?
//			// May be if neither destinationReference nor
//			// originationReference is needed
//			// then no userInfo is needed (there is an
//			// ApplicationContextName list in the specification)
//
//			// TODO: Make a checking if MAP-OPEN is not needed -> continue
//			// without sending TC-U-ABORT - how?
//		} else {
//			// if an application-context-name different from version 1 is
//			// included in the primitive and if User-
//			// information is present, the User-information must constitute
//			// a syntactically correct MAP-OPEN
//			// dialogue PDU. Otherwise a TC-U-ABORT request primitive with
//			// abort-reason "User-specific" and
//			// user-information "MAP-ProviderAbortInfo" indicating
//			// "abnormalDialogue" shall be issued and the
//			// local MAP-user shall not be informed.
//
//			MAPOpenInfoImpl mapOpenInfoImpl = new MAPOpenInfoImpl();
//
//			if (!userInfo.isOid()) {
//				loger.error("When parsing TC-BEGIN: userInfo.isOid() check failed");
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e) {
//					loger.error("Error while firing TC-U-ABORT. ", e);
//				}
//				return;
//			}
//
//			long[] oid = userInfo.getOidValue();
//
//			MAPDialogueAS mapDialAs = MAPDialogueAS.getInstance(oid);
//
//			if (mapDialAs == null) {
//				loger.error("When parsing TC-BEGIN: Expected MAPDialogueAS.MAP_DialogueAS but is null");
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e) {
//					loger.error("Error while firing TC-U-ABORT. ", e);
//				}
//				return;
//			}
//
//			if (!userInfo.isAsn()) {
//				loger.error("When parsing TC-BEGIN: userInfo.isAsn() check failed");
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e) {
//					loger.error("Error while firing TC-U-ABORT. ", e);
//				}
//				return;
//			}
//
//			try {
//				byte[] asnData = userInfo.getEncodeType();
//
//				AsnInputStream ais = new AsnInputStream(asnData);
//
//				int tag = ais.readTag();
//
//				// It should be MAP_OPEN Tag
//				if (tag != MAPOpenInfoImpl.MAP_OPEN_INFO_TAG) {
//					loger.error("When parsing TC-BEGIN: MAP-OPEN dialog PDU must be received");
//					try {
//						this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//					} catch (MAPException e) {
//						loger.error("Error while firing TC-U-ABORT. ", e);
//					}
//					return;
//				}
//
//				mapOpenInfoImpl.decodeAll(ais);
//
//				destReference = mapOpenInfoImpl.getDestReference();
//				origReference = mapOpenInfoImpl.getOrigReference();
//				extensionContainer = mapOpenInfoImpl.getExtensionContainer();
//			} catch (AsnException e) {
//				e.printStackTrace();
//				loger.error("AsnException when parsing MAP-OPEN Pdu: " + e.getMessage(), e);
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e1) {
//					loger.error("Error while firing TC-U-ABORT. ", e1);
//				}
//				return;
//			} catch (IOException e) {
//				e.printStackTrace();
//				loger.error("IOException when parsing MAP-OPEN Pdu: " + e.getMessage());
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e1) {
//					loger.error("Error while firing TC-U-ABORT. ", e1);
//				}
//				return;
//			} catch (MAPParsingComponentException e) {
//				e.printStackTrace();
//				loger.error("MAPException when parsing MAP-OPEN Pdu: " + e.getMessage());
//				try {
//					this.fireTCAbortProvider(tcBeginIndication.getDialog(), MAPProviderAbortReason.abnormalDialogue, null);
//				} catch (MAPException e1) {
//					loger.error("Error while firing TC-U-ABORT. ", e1);
//				}
//				return;
//			}
//		}
//
//		// if an application-context-name different from version 1 is
//		// received in a syntactically correct TC-
//		// BEGIN indication primitive but is not acceptable from a load
//		// control point of view, the MAP PM
//		// shall ignore this dialogue request. The MAP-user is not informed.
//		// TODO: Checking if MAP PM is overloaded - if so - reject some less
//		// important ApplicationContexts
//		// without sending any responses and MAP user informing
//
//		// Selecting the MAP service that can perform the ApplicationContext
//		if (perfSer == null) {
//			for (MAPServiceBase ser : this.mapServices) {
//
//				ServingCheckData chkRes = ser.isServingService(mapAppCtx);
//				switch (chkRes.getResult()) {
//				case AC_Serving:
//					perfSer = ser;
//					break;
//
//				case AC_VersionIncorrect:
//					try {
//						this.fireTCAbortACNNotSupported(tcBeginIndication.getDialog(), null, chkRes.getAlternativeApplicationContext());
//					} catch (MAPException e1) {
//						loger.error("Error while firing TC-U-ABORT. ", e1);
//					}
//					break;
//				}
//
//				if (perfSer != null)
//					break;
//			}
//		}
//
//		// No MAPService can accept the received ApplicationContextName
//		if (perfSer == null) {
//			StringBuffer s = new StringBuffer();
//			s.append("Unsupported ApplicationContextName is received: ");
//			for (long l : acn.getOid()) {
//				s.append(l).append(", ");
//			}
//
//			loger.error(s.toString());
//			try {
//				this.fireTCAbortACNNotSupported(tcBeginIndication.getDialog(), null, null);
//			} catch (MAPException e1) {
//				loger.error("Error while firing TC-U-ABORT. ", e1);
//			}
//
//			return;
//		}
//
//		// MAPService is not activated
//		if (!perfSer.isActivated()) {
//			StringBuffer s = new StringBuffer();
//			s.append("ApplicationContextName of not activated MAPService is received: ");
//			for (long l : acn.getOid()) {
//				s.append(l).append(", ");
//			}
//
//			try {
//				this.fireTCAbortACNNotSupported(tcBeginIndication.getDialog(), null, null);
//			} catch (MAPException e1) {
//				loger.error("Error while firing TC-U-ABORT. ", e1);
//			}
//		}
//
//		MAPDialogImpl mapDialogImpl = ((MAPServiceBaseImpl) perfSer).createNewDialogIncoming(mapAppCtx, tcBeginIndication.getDialog());
//		synchronized (mapDialogImpl) {
//			this.addDialog(mapDialogImpl);
//
//			mapDialogImpl.setState(MAPDialogState.InitialReceived);
//
//			this.deliverDialogRequest(mapDialogImpl, destReference, origReference, extensionContainer);
//			if (mapDialogImpl.getState() == MAPDialogState.Expunged)
//				// The Dialog was aborter or refused
//				return;
//
//			// Now let us decode the Components
//			if (comps != null) {
//				processComponents(mapDialogImpl, comps);
//			}
//
//			this.deliverDialogDelimiter(mapDialogImpl);
//		}
	}	
	
	public void onTCContinue(TCContinueIndication tcContinueIndication) {
		// TODO .......................
		// .....................................
	}	
	
	public void onTCEnd(TCEndIndication tcEndIndication) {
		// TODO .......................
		// .....................................
	}	

	public void onTCUni(TCUniIndication arg0) {
	}
	
	@Override
	public void onInvokeTimeout(Invoke invoke) {

		CAPDialogImpl capDialogImpl = (CAPDialogImpl) this.getCAPDialog(((InvokeImpl) invoke).getDialog().getDialogId());

		if (capDialogImpl != null) {
			synchronized (capDialogImpl) {
				if (capDialogImpl.getState() != CAPDialogState.Expunged && !capDialogImpl.getNormalDialogShutDown()) {

					// Getting the CAP Service that serves the CAP Dialog
					CAPServiceBaseImpl perfSer = (CAPServiceBaseImpl)capDialogImpl.getService();
					
					// Check if the InvokeTimeout in this situation is normal (may be for a class 2,3,4 components)
					// TODO: ................................
					
					perfSer.deliverInvokeTimeout(capDialogImpl, invoke);
				}
			}
		}
	}

	@Override
	public void onDialogTimeout(Dialog tcapDialog) {
		
		CAPDialogImpl capDialogImpl = (CAPDialogImpl) this.getCAPDialog(tcapDialog.getDialogId());

		if (capDialogImpl != null) {
			synchronized (capDialogImpl) {
				if (capDialogImpl.getState() != CAPDialogState.Expunged && !capDialogImpl.getNormalDialogShutDown()) {

					this.deliverDialogTimeout(capDialogImpl);
				}
			}
		}
	}

	@Override
	public void onDialogReleased(Dialog tcapDialog) {

		CAPDialogImpl capDialogImpl = (CAPDialogImpl) this.getCAPDialog(tcapDialog.getDialogId());

		if (capDialogImpl != null) {
			synchronized (capDialogImpl) {
				if (capDialogImpl.getState() != CAPDialogState.Expunged && !capDialogImpl.getNormalDialogShutDown()) {

					// TCAP Dialog is destroyed when CapDialog is alive and not shutting down
					capDialogImpl.setNormalDialogShutDown();
					this.deliverDialogUserAbort(capDialogImpl, CAPGeneralAbortReason.BadReasonData, null);
					
					capDialogImpl.setState(CAPDialogState.Expunged);
				}
			}
		}
	}

	public void onTCPAbort(TCPAbortIndication tcPAbortIndication) {
		// TODO .......................
		// ....................................
	}

	public void onTCUserAbort(TCUserAbortIndication tcUserAbortIndication) {
		// TODO .......................
		// ....................................
	}

	private void processComponents(CAPDialogImpl capDialogImpl, Component[] components) {

		// Getting the CAP Service that serves the CAP Dialog
		CAPServiceBaseImpl perfSer = (CAPServiceBaseImpl)capDialogImpl.getService();

		// Now let us decode the Components
		for (Component c : components) {

			try {
				ComponentType compType = c.getType();

				Long invokeId = c.getInvokeId();

				Parameter parameter;
				OperationCode oc;
				Long linkedId = 0L;
				
				switch (compType) {
				case Invoke: {
					Invoke comp = (Invoke) c;
					oc = comp.getOperationCode();
					parameter = comp.getParameter();
					linkedId = comp.getLinkedId();
					
					// Checking if the invokeId is not duplicated
					if (!capDialogImpl.addIncomingInvokeId(invokeId)) {
						this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.DuplicatedInvokeIdReceived);
						
						Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
						problem.setInvokeProblemType(InvokeProblemType.DuplicateInvokeID);
						capDialogImpl.sendRejectComponent(null, problem);

						return;
					}
					
					if (linkedId != null) {
						// linkedId exists Checking if the linkedId exists
						if (!capDialogImpl.checkIncomingInvokeIdExists(linkedId)) {
							this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.UnknownLinkedIdReceived);

							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
							problem.setInvokeProblemType(InvokeProblemType.UnrechognizedLinkedID);
							capDialogImpl.sendRejectComponent(invokeId, problem);

							return;
						}
					}
				}
					break;

				case ReturnResult: {
					// ReturnResult is not supported by CAMEL
					this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.AbnormalComponentReceivedFromThePeer);
					
					Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
					problem.setInvokeProblemType(InvokeProblemType.UnrecognizedOperation);
					capDialogImpl.sendRejectComponent(null, problem);

					return;
				}

				case ReturnResultLast: {
					ReturnResultLast comp = (ReturnResultLast) c;
					oc = comp.getOperationCode();
					parameter = comp.getParameter();
				}
					break;

				case ReturnError: {
					ReturnError comp = (ReturnError) c;
					
					long errorCode = 0;
					if (comp.getErrorCode() != null && comp.getErrorCode().getErrorType() == ErrorCodeType.Local)
						errorCode = comp.getErrorCode().getLocalErrorCode();
					if (errorCode < CAPErrorCode.minimalCodeValue || errorCode > CAPErrorCode.maximumCodeValue) {
						// Not Local error code and not CAP error code received
						perfSer.deliverProviderErrorComponent(capDialogImpl, invokeId, CAPComponentErrorReason.InvalidErrorComponentReceived);

						Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.ReturnError);
						problem.setReturnErrorProblemType(ReturnErrorProblemType.UnrecognizedError);
						capDialogImpl.sendRejectComponent(invokeId, problem);
						
						return;
					}
					
					CAPErrorMessage msgErr = this.capErrorMessageFactory.createMessageFromErrorCode(errorCode);
					try {
						Parameter p = comp.getParameter();
						if (p != null && p.getData() != null) {
							byte[] data = p.getData();
							AsnInputStream ais = new AsnInputStream(data, p.getTagClass(), p.isPrimitive(), p.getTag());
							((CAPErrorMessageImpl)msgErr).decodeData(ais, data.length);
						}
					} catch ( CAPParsingComponentException e) {
						// Failed when parsing the component - send TC-U-REJECT
						perfSer.deliverProviderErrorComponent(capDialogImpl, invokeId, CAPComponentErrorReason.InvalidErrorComponentReceived);

						Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.ReturnError);
						problem.setReturnErrorProblemType(ReturnErrorProblemType.MistypedParameter);
						capDialogImpl.sendRejectComponent(invokeId, problem);

						return;
					}
					perfSer.deliverErrorComponent(capDialogImpl, comp.getInvokeId(), msgErr);
					
					return;
				}

				case Reject: {
					Reject comp = (Reject) c;
					perfSer.deliverRejectComponent(capDialogImpl, comp.getInvokeId(), comp.getProblem());
					
					return;
				}
				
				default:
					return;
				}
				
				try {
					
					perfSer.processComponent(compType, oc, parameter, capDialogImpl, invokeId, linkedId);
					
				} catch (CAPParsingComponentException e) {
					
					loger.error("CAPParsingComponentException when parsing components: " + e.getReason().toString() + " - " + e.getMessage(), e);
					
					switch (e.getReason()) {
					case UnrecognizedOperation:
						// Component does not supported - send TC-U-REJECT
						if (compType == ComponentType.Invoke) {
							this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.UnrecognizedOperation);

							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
							problem.setInvokeProblemType(InvokeProblemType.UnrecognizedOperation);
							capDialogImpl.sendRejectComponent(invokeId, problem);
						} else {
							perfSer.deliverProviderErrorComponent(capDialogImpl, invokeId, CAPComponentErrorReason.UnrecognizedOperation);
							
							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.ReturnResult);
							problem.setReturnResultProblemType(ReturnResultProblemType.MistypedParameter);
							capDialogImpl.sendRejectComponent(invokeId, problem);
						}
						break;

					case MistypedParameter:
						// Failed when parsing the component - send TC-U-REJECT
						if (compType == ComponentType.Invoke) {
							this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.AbnormalComponentReceivedFromThePeer);
							
							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
							problem.setInvokeProblemType(InvokeProblemType.MistypedParameter);
							capDialogImpl.sendRejectComponent(invokeId, problem);
						} else {
							if (compType == ComponentType.Reject)
								perfSer.deliverProviderErrorComponent(capDialogImpl, invokeId, CAPComponentErrorReason.InvalidRejectReceived);
							else
								perfSer.deliverProviderErrorComponent(capDialogImpl, invokeId, CAPComponentErrorReason.InvalidReturnResultComponentReceived);
							
							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.ReturnResult);
							problem.setReturnResultProblemType(ReturnResultProblemType.MistypedParameter);
							capDialogImpl.sendRejectComponent(invokeId, problem);
						}
						break;

					case LinkedResponseUnexpected:
						// Failed when parsing the component - send TC-U-REJECT
						if (compType == ComponentType.Invoke) {
							this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.LinkedResponseUnexpected);
							
							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
							problem.setInvokeProblemType(InvokeProblemType.LinkedResponseUnexpected);
							capDialogImpl.sendRejectComponent(invokeId, problem);
						}
						break;

					case UnexpectedLinkedOperation:
						// Failed when parsing the component - send TC-U-REJECT
						if (compType == ComponentType.Invoke) {
							this.deliverDialogNotice(capDialogImpl, CAPNoticeProblemDiagnostic.UnexpectedLinkedOperation);
							
							Problem problem = this.getTCAPProvider().getComponentPrimitiveFactory().createProblem(ProblemType.Invoke);
							problem.setInvokeProblemType(InvokeProblemType.UnexpectedLinkedOperation);
							capDialogImpl.sendRejectComponent(invokeId, problem);
						}
						break;
					}

				}
			} catch (CAPException e) {
				loger.error("Error sending the RejectComponent: " + e.getMessage(), e);
			}
		} 
	}

	private void deliverDialogDelimiter(CAPDialog capDialog) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogDelimiter(capDialog);
		}
	}

	private void deliverDialogRequest(CAPDialog capDialog, CAPGprsReferenceNumber capGprsReferenceNumber) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogRequest(capDialog, capGprsReferenceNumber);
		}
	}

	private void deliverDialogAccept(CAPDialog capDialog, CAPGprsReferenceNumber capGprsReferenceNumber) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogAccept(capDialog, capGprsReferenceNumber);
		}
	}

	private void deliverDialogUserAbort(CAPDialog capDialog, CAPGeneralAbortReason generalReason, CAPUserAbortReason userReason) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogUserAbort(capDialog, generalReason, userReason);
		}
	}

	private void deliverDialogProviderAbort(CAPDialog capDialog, PAbortCauseType abortCause) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogProviderAbort(capDialog, abortCause);
		}
	}

	private void deliverDialogClose(CAPDialog capDialog) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogClose(capDialog);
		}
	}

	protected void deliverDialogResease(CAPDialog capDialog) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogResease(capDialog);
		}
	}

	protected void deliverDialogTimeout(CAPDialog capDialog) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogTimeout(capDialog);
		}
	}
	
	protected void deliverDialogNotice(CAPDialog capDialog, CAPNoticeProblemDiagnostic noticeProblemDiagnostic) {
		for (CAPDialogListener listener : this.dialogListeners) {
			listener.onDialogNotice(capDialog, noticeProblemDiagnostic);
		}
	}

	protected void fireTCBegin(Dialog tcapDialog, ApplicationContextName acn, CAPGprsReferenceNumber gprsReferenceNumber) throws CAPException {

		TCBeginRequest tcBeginReq = encodeTCBegin(tcapDialog, acn, gprsReferenceNumber);

		try {
			tcapDialog.send(tcBeginReq);
		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}

	}

	protected TCBeginRequest encodeTCBegin(Dialog tcapDialog, ApplicationContextName acn, CAPGprsReferenceNumber gprsReferenceNumber) throws CAPException {
		
		TCBeginRequest tcBeginReq = this.getTCAPProvider().getDialogPrimitiveFactory().createBegin(tcapDialog);

		tcBeginReq.setApplicationContextName(acn);

		if (gprsReferenceNumber != null) {
			AsnOutputStream localasnOs = new AsnOutputStream();
			((CAPGprsReferenceNumberImpl)gprsReferenceNumber).encodeAll(localasnOs);

			UserInformation userInformation = TcapFactory.createUserInformation();

			userInformation.setOid(true);
			userInformation.setOidValue(CAPGprsReferenceNumberImpl.CAP_Dialogue_OId);

			userInformation.setAsn(true);
			userInformation.setEncodeType(localasnOs.toByteArray());

			tcBeginReq.setUserInformation(userInformation);
		}
		return tcBeginReq;
	}

	protected void fireTCContinue(Dialog tcapDialog, ApplicationContextName acn, CAPGprsReferenceNumber gprsReferenceNumber)
			throws CAPException {

		TCContinueRequest tcContinueReq = encodeTCContinue(tcapDialog, acn, gprsReferenceNumber);

		try {
			tcapDialog.send(tcContinueReq);
		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}

	protected TCContinueRequest encodeTCContinue(Dialog tcapDialog, ApplicationContextName acn, CAPGprsReferenceNumber gprsReferenceNumber) throws CAPException {
		TCContinueRequest tcContinueReq = this.getTCAPProvider().getDialogPrimitiveFactory().createContinue(tcapDialog);

		if (acn != null)
			tcContinueReq.setApplicationContextName(acn);

		if (gprsReferenceNumber != null) {

			AsnOutputStream localasnOs = new AsnOutputStream();
			((CAPGprsReferenceNumberImpl)gprsReferenceNumber).encodeAll(localasnOs);

			UserInformation userInformation = TcapFactory.createUserInformation();

			userInformation.setOid(true);
			userInformation.setOidValue(CAPGprsReferenceNumberImpl.CAP_Dialogue_OId);

			userInformation.setAsn(true);
			userInformation.setEncodeType(localasnOs.toByteArray());

			tcContinueReq.setUserInformation(userInformation);
		}
		return tcContinueReq;
	}

	protected void fireTCEnd(Dialog tcapDialog, boolean prearrangedEnd, ApplicationContextName acn, CAPGprsReferenceNumber gprsReferenceNumber)
			throws CAPException {

		TCEndRequest endRequest = encodeTCEnd(tcapDialog, prearrangedEnd, acn, gprsReferenceNumber);

		try {
			tcapDialog.send(endRequest);
		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}

	protected TCEndRequest encodeTCEnd(Dialog tcapDialog, boolean prearrangedEnd, ApplicationContextName acn, CAPGprsReferenceNumber gprsReferenceNumber) throws CAPException {
		TCEndRequest endRequest = this.getTCAPProvider().getDialogPrimitiveFactory().createEnd(tcapDialog);

		if (!prearrangedEnd) {
			endRequest.setTermination(TerminationType.Basic);
		} else {
			endRequest.setTermination(TerminationType.PreArranged);
		}

		if (acn != null)
			endRequest.setApplicationContextName(acn);

		if (gprsReferenceNumber != null) {

			AsnOutputStream localasnOs = new AsnOutputStream();
			((CAPGprsReferenceNumberImpl)gprsReferenceNumber).encodeAll(localasnOs);

			UserInformation userInformation = TcapFactory.createUserInformation();

			userInformation.setOid(true);
			userInformation.setOidValue(CAPGprsReferenceNumberImpl.CAP_Dialogue_OId);

			userInformation.setAsn(true);
			userInformation.setEncodeType(localasnOs.toByteArray());

			endRequest.setUserInformation(userInformation);
		}
		return endRequest;
	}

	protected void fireTCAbort(Dialog tcapDialog, CAPGeneralAbortReason generalAbortReason, CAPUserAbortReason userAbortReason) throws CAPException {

		TCUserAbortRequest tcUserAbort = this.getTCAPProvider().getDialogPrimitiveFactory().createUAbort(tcapDialog);

		switch(generalAbortReason){
		case ACNNotSupported:
			tcUserAbort.setDialogServiceUserType(DialogServiceUserType.AcnNotSupported);
			tcUserAbort.setApplicationContextName(tcapDialog.getApplicationContextName());
			break;
			
		case DialogRefused:
		case BadReasonData:
		case TcapDialogDestroyedData:
			tcUserAbort.setDialogServiceUserType(DialogServiceUserType.NoReasonGive);
			tcUserAbort.setApplicationContextName(tcapDialog.getApplicationContextName());
			break;
			
		case UserSpecific:
			if (userAbortReason == null)
				userAbortReason = CAPUserAbortReason.no_reason_given;
			CAPUserAbortPrimitiveImpl abortReasonPrimitive = new CAPUserAbortPrimitiveImpl(userAbortReason);
			AsnOutputStream localasnOs = new AsnOutputStream();
			abortReasonPrimitive.encodeAll(localasnOs);

			UserInformation userInformation = TcapFactory.createUserInformation();
			userInformation.setOid(true);
			userInformation.setOidValue(CAPUserAbortPrimitiveImpl.CAP_AbortReason_OId);
			userInformation.setAsn(true);
			userInformation.setEncodeType(localasnOs.toByteArray());

			tcUserAbort.setUserInformation(userInformation);
			break;
		}

		try {
			tcapDialog.send(tcUserAbort);
		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}
}

