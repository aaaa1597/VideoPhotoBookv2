#include "VuforiaController.h"
#include <format>

extern void garnishLog(const std::string &logstr);

ErrorCode VuforiaController::initAR(const std::string &licensekey) {
    garnishLog(std::format("VuforiaController::initAR() start{}", "."));

    return ErrorCode::None;
}
