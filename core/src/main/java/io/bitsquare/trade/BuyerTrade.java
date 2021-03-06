/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.trade;

import io.bitsquare.app.Version;
import io.bitsquare.btc.FeePolicy;
import io.bitsquare.common.handlers.ErrorMessageHandler;
import io.bitsquare.common.handlers.ResultHandler;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.storage.Storage;
import io.bitsquare.trade.offer.Offer;
import io.bitsquare.trade.protocol.trade.BuyerProtocol;
import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BuyerTrade extends Trade {
    // That object is saved to disc. We need to take care of changes to not break deserialization.
    private static final long serialVersionUID = Version.LOCAL_DB_VERSION;

    private static final Logger log = LoggerFactory.getLogger(BuyerAsOffererTrade.class);

    BuyerTrade(Offer offer, Coin tradeAmount, long tradePrice, NodeAddress tradingPeerNodeAddress, Storage<? extends TradableList> storage) {
        super(offer, tradeAmount, tradePrice, tradingPeerNodeAddress, storage);
    }

    BuyerTrade(Offer offer, Storage<? extends TradableList> storage) {
        super(offer, storage);
    }

    @Override
    protected void initStates() {
        if (state == null)
            state = State.PREPARATION;
    }

    public void onFiatPaymentStarted(ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        checkArgument(tradeProtocol instanceof BuyerProtocol, "Check failed:  tradeProtocol instanceof BuyerProtocol");
        ((BuyerProtocol) tradeProtocol).onFiatPaymentStarted(resultHandler, errorMessageHandler);
    }

    @Override
    public void reSendConfirmation() {
        if (state == Trade.State.BUYER_SENT_FIAT_PAYMENT_INITIATED_MSG) {
            log.info("reSendConfirmation onFiatPaymentStarted");
            onFiatPaymentStarted(() -> log.debug("onFiatPaymentStarted succeeded"),
                    log::warn);
        }
    }

    @Override
    public Coin getPayoutAmount() {
        checkNotNull(getTradeAmount(), "Invalid state: getTradeAmount() = null");

        return FeePolicy.getSecurityDeposit().add(getTradeAmount());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Setter for Mutable objects
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setState(State state) {
        super.setState(state);

        if (state == State.WITHDRAW_COMPLETED && tradeProtocol != null)
            tradeProtocol.completed();
    }

}
