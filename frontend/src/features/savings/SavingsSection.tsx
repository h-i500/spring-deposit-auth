import Card from "../../components/Card";
import AccountList from "./AccountList";
import TransferForm from "./TransferForm";

export default function SavingsSection() {
  return (
    <>
      <Card title="普通預金（Savings）アカウント">
        <AccountList />
      </Card>

      <Card title="普通預金の振替（任意：運用に合わせて使用）">
        <TransferForm />
      </Card>
    </>
  );
}
