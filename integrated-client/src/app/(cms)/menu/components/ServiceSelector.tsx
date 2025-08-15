import { Select } from "@chakra-ui/react";
import React from "react";

interface ServiceSelectorProps {
  selectedService: string;
  onServiceChange: (serviceId: string) => void;
}

export const ServiceSelector: React.FC<ServiceSelectorProps> = ({
  selectedService,
  onServiceChange,
}) => {
  return (
    <Select
      value={selectedService}
      onChange={(e) => onServiceChange(e.target.value)}
      width="200px"
      size="sm"
    >
      <option value="">통합 CMS</option>
      <option value="douzone">더존 CMS</option>
    </Select>
  );
};
