package com.fullaccel.paircarserver.settlement.application

import com.fullaccel.paircarserver.global.error.BusinessException
import com.fullaccel.paircarserver.global.error.ErrorCode
import com.fullaccel.paircarserver.settlement.application.dto.ReceiptInquiryResult
import com.fullaccel.paircarserver.settlement.application.event.ReceiptInquiryCreatedEvent
import com.fullaccel.paircarserver.settlement.domain.Receipt
import com.fullaccel.paircarserver.settlement.domain.ReceiptInquiry
import com.fullaccel.paircarserver.settlement.domain.ReceiptInquiryRepository
import com.fullaccel.paircarserver.settlement.domain.ReceiptRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class ReceiptInquiryServiceImpl(
    private val receiptRepository: ReceiptRepository,
    private val receiptInquiryRepository: ReceiptInquiryRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : ReceiptInquiryService {

    @Transactional
    override fun createInquiry(requesterMemberId: Long, receiptId: Long, message: String): ReceiptInquiryResult {
        val receipt = getReceipt(receiptId)
        if (receipt.payer.member.id != requesterMemberId) throw BusinessException(ErrorCode.RECEIPT_ACCESS_DENIED)
        val inquiry = receiptInquiryRepository.save(ReceiptInquiry(receipt = receipt, requester = receipt.payer, message = message))
        eventPublisher.publishEvent(
            ReceiptInquiryCreatedEvent(
                receiptId = receipt.id,
                inquiryId = inquiry.id,
                month = YearMonth.from(receipt.month),
                payeeMemberId = receipt.payee.member.id,
                requesterNickname = receipt.payer.member.nickname,
                message = message,
            ),
        )
        return ReceiptInquiryResult.from(inquiry)
    }

    override fun getInquiries(requesterMemberId: Long, receiptId: Long): List<ReceiptInquiryResult> {
        val receipt = getReceipt(receiptId)
        if (requesterMemberId != receipt.payer.member.id && requesterMemberId != receipt.payee.member.id) {
            throw BusinessException(ErrorCode.RECEIPT_ACCESS_DENIED)
        }
        return receiptInquiryRepository.findAllByReceiptIdOrderById(receiptId).map(ReceiptInquiryResult::from)
    }

    @Transactional
    override fun resolveInquiry(requesterMemberId: Long, receiptId: Long, inquiryId: Long): ReceiptInquiryResult {
        val receipt = getReceipt(receiptId)
        if (receipt.payee.member.id != requesterMemberId) throw BusinessException(ErrorCode.RECEIPT_ACCESS_DENIED)
        val inquiry = receiptInquiryRepository.findByIdOrNull(inquiryId)?.takeIf { it.receipt.id == receiptId }
            ?: throw BusinessException(ErrorCode.RECEIPT_INQUIRY_NOT_FOUND)
        inquiry.resolve()
        return ReceiptInquiryResult.from(inquiry)
    }

    private fun getReceipt(receiptId: Long): Receipt =
        receiptRepository.findByIdOrNull(receiptId) ?: throw BusinessException(ErrorCode.RECEIPT_NOT_FOUND)
}
