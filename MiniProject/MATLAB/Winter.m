clear; clc;

InputDataWinter;

figure(1)
clf
% plot(Time,CO2)
% hold on
% plot(Time, Temp)
% plot(Time, Wind)

[hAx,hLine1,hLine2] = plotyy(Time,CO2,Time,Temp)
hAx(1).XTickLabelRotation = -30
hold on

title('Data for December 5th')
xlabel('Time')

ylabel(hAx(1),'gram CO_2 / kWh') % left y-axis
ylabel(hAx(2),'Temperature [C]') % right y-axis

m_CO2 = mean(CO2)
plot(Time,ones(size(CO2))*m_CO2, '--k')   % Dayly mean
plot(Time,ones(size(CO2))*340, '-.b')     % Yearly mean



grid minor
legend(...
    'CO_2 emission [gram CO_2/kWH]',...
    'Daily mean CO_2', ...
    'Yearly mean CO_2',...
    'Temperature [C]',...
    'Location', 'northeast')

scale = 0.1;
pos = get(gca, 'Position');
pos(2) = pos(2)+scale*pos(4);
pos(4) = (1-scale)*pos(4);
set(gca, 'Position', pos)