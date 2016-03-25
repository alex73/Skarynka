--[[
GENERATED PROPCASE TABLE
use propcase.lua instead
--]]
return {
  AE_LOCK=3,          -- 0 = AE not locked, 1 = AE locked
  AF_ASSIST_BEAM=5,          -- 0=disabled,  1=enabled
  REAL_FOCUS_MODE=6,          --??? WIKI|Propcase focus_mode
  AF_FRAME=8,          -- 1 = FlexiZone, 2 = Face AiAF / Tracking AF
  AF_LOCK=11,         -- 0 = AF not locked, 1 = AF locked
  CONTINUOUS_AF=12,         -- 0 = Continuous AF off, 1 = Continuous AF on
  FOCUS_STATE=18,         --???
  AV2=22,         -- (philmoz, May 2011) - this value causes overrides to be saved in JPEG and shown on Canon OSD
  AV=23,         -- This values causes the actual aperture value to be overriden
  MIN_AV=25,
  USER_AV=26,
  BRACKET_MODE=29,
  BV=34,
  SHOOTING_MODE=49,
  CUSTOM_SATURATION=55,         -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  QUALITY=57,
  CUSTOM_CONTRAST=59,         -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  FLASH_SYNC_CURTAIN=64,
  SUBJECT_DIST2=65,
  DATE_STAMP=66,         -- 0 = Off, 1 = Date, 2 = Date & Time
  DELTA_SV=79,
  DIGITAL_ZOOM_MODE=91,         -- Digital Zoom Mode/State 0 = off/standard, 2 = 1.5x, 3 = 2.0x
  DIGITAL_ZOOM_STATE=94,         -- Digital Zoom Mode/State 0 = Digital Zoom off, 1 = Digital Zoom on
  DIGITAL_ZOOM_POSITION=95,
  DRIVE_MODE=102,
  OVEREXPOSURE=103,
  DISPLAY_MODE=105,
  EV_CORRECTION_1=107,
  FLASH_ADJUST_MODE=121,
  FLASH_FIRE=122,
  FLASH_EXP_COMP=127,    -- APEX96 units
  FOCUS_MODE=133,
  FLASH_MANUAL_OUTPUT=141,        -- !not sure, but required for compile; from propset4
  FLASH_MODE=143,        -- 0 = Auto, 1 = ON, 2 = OFF
  IS_MODE=145,        -- 0 = Continuous, 2 = only Shoot, 4 = OFF
  ISO_MODE=149,
  METERING_MODE=157,
  CUSTOM_BLUE=177,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  CUSTOM_GREEN=178,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  CUSTOM_RED=179,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  CUSTOM_SKIN_TONE=180,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  MY_COLORS=188,        -- 0 = Off, 1 = Vivid, 2 = Neutral, 3 = B/W, 4 = Sepia, 5 = Positive Film, 6 = Lighter Skin Tone, 7 = Darker Skin Tone, 8 = Vivid Red, 9 = Vivid Green, 10 = Vivid Blue, 11 = Custom Color
  ND_FILTER_STATE=196,        -- 0 = out, 1 = in
  OPTICAL_ZOOM_POSITION=199,
  EXPOSURE_LOCK=210,     -- Old PROPCASE_SHOOTING value - gets set when set_aelock called or AEL button pressed
  SHOOTING=303,     -- This value appears to work better - gets set to 1 when camera has focused and set exposure, returns to 0 after shot
  EV_CORRECTION_2=211,
  IS_FLASH_READY=212,
  RESOLUTION=222,        -- 0 = L, 1 = M1, 2 = M2, 4 = S, 7 = Low Light
  ORIENTATION_SENSOR=223,
  TIMER_MODE=227,        -- 0 = OFF, 1 = 2 sec, 2 = 10 sec, 3 = Custom
  TIMER_DELAY=228,        -- timer delay in msec
  CUSTOM_SHARPNESS=229,        -- Canon Menu slide bar values: 255, 254, 0, 1, 2
  STITCH_DIRECTION=237,        -- 0=left>right, 1=right>left. Some cams have more
  STITCH_SEQUENCE=242,        -- counts shots in stitch sequence, positive=left>right, negative=right>left
  SUBJECT_DIST1=249,
  SV_MARKET=250,
  TV2=265,        -- (philmoz, May 2011) - this value causes overrides to be saved in JPEG and shown on Canon OSD
  TV=266,        -- Need to set this value for overrides to work correctly
  USER_TV=268,
  WB_MODE=272,        -- 0 = Auto, 1 = Daylight, 2 = Cloudy, 3 = Tungsten, 4 = Fluorescent, 5 = Fluorescent H, 7 = Custom
  WB_ADJ=273,
  SERVO_AF=299,        -- 0 = Servo AF off, 1 = Servo AF on
  ASPECT_RATIO=300,        -- 0 = 4:3, 1 = 16:9, 2 = 3:2, 3 = 1:1
  SV=347,        -- (philmoz, May 2011) - this value causes overrides to be saved in JPEG and shown on Canon OSD
  GPS=358,        -- (CHDKLover, August 2011) - contains a 272 bytes long structure
  TIMER_SHOTS=377,        -- Number of shots for TIMER_MODE=Custom
}
