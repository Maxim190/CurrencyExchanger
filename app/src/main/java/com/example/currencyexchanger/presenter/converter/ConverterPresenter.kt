package com.example.currencyexchanger.presenter.converter

import com.example.currencyexchanger.R
import com.example.currencyexchanger.model.Storage
import com.example.currencyexchanger.model.pojo.Valute
import com.example.currencyexchanger.view.converter.ConverterViewInterface
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ConverterPresenter(val view: ConverterViewInterface): ConverterPresenterInterface,
    Storage.AutoDataUpdateNotificationsListener {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")
    private val module: Storage = Storage.instance
    private var charCodeFrom: String = "RUR"
    private var charCodeTo: String = "USD"

    init {
        module.subscribeOnAutoUpdNotifications(this)
        setDataToSpinners()
        displayDate()
    }

    private fun setDataToSpinners() {
        val data = module.getData()
        if (!data.valutes.containsKey("RUR")) {
            data.valutes["RUR"] = Valute("RUR", 1, "Российский рубль", 1.0)
        }

        val spinnerStrings = data.valutes.map {
                entry -> entry.value.charCode + " " + entry.value.name
        }.toMutableList()

        runBlocking {
            view.setDataToSpinners(spinnerStrings)

            view.setSelectionToSpinnerFrom(
                spinnerStrings.indexOf(data.valutes[charCodeFrom].toString()))
            view.setSelectionToSpinnerTo(
                spinnerStrings.indexOf(data.valutes[charCodeTo].toString()))
        }
    }

    override fun newValuteSelected() {
        charCodeFrom = getCharCodeFromStr(view.getSelectedSpinnerFromItem())
        charCodeTo = getCharCodeFromStr(view.getSelectedSpinnerToItem())

        val rateFrom = convert(charCodeFrom, charCodeTo)
        val rateTo = convert(charCodeTo, charCodeFrom)

        view.setRateFrom("1 $charCodeFrom = %.4f $charCodeTo".format(rateFrom))
        view.setRateTo("1 $charCodeTo = %.4f $charCodeFrom".format(rateTo))

        convertNumFromView(view.getNumToConvert(), R.id.num_to_convert)
    }

    override fun convertNumFromView(strWithNum: CharSequence?, viewId: Int) {
        strWithNum?.toString().run {
            val num = if (this!!.isEmpty()) 0.0 else this.toDouble()

            val result: Double
            when (viewId) {
                R.id.num_to_convert -> {
                    result = convert(charCodeFrom, charCodeTo, num)
                    view.setConvertedNum(decFormat(result))
                }
                R.id.num_converted -> {
                    result = convert(charCodeTo, charCodeFrom, num)
                    view.setNumToConvert(decFormat(result))
                }
            }
        }
    }

    private fun decFormat(value: Double) = "%.4f".format(value).replace(',', '.')

    private fun getCharCodeFromStr(str: String) = str.split(" ")[0]

    private fun convert(charCodeFrom: String, charCodeTo: String, convertValue: Double = 1.0) =
        convert (module.getData().valutes[charCodeFrom]!!,
            module.getData().valutes[charCodeTo]!!, convertValue)

    private fun convert(valuteFrom: Valute, valuteTo: Valute, convertValue: Double) =
        (valuteFrom.value / valuteFrom.nominal / valuteTo.value) * convertValue

    override fun onStorageAutomaticallyUpdated() {
        displayDate()
    }

    private fun displayDate() {
        val date = module.getData().date
        view.displayDate(ZonedDateTime.parse(date).format(dateFormatter))
    }
}